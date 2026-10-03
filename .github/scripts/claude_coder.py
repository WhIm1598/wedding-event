#!/usr/bin/env python3
"""
Spec-to-code agent helper around the Claude Code CLI (`claude -p`, run by .github/workflows/claude-agent.yml).

The workflow runs three steps:
  1. `prepare`  — builds the task prompt: what changed in the PO spec, which modules are active, their
                  conventions and the rules (Claude Code then reads the spec and the code itself).
                  No change under docs/features since the base commit => has_changes=false, nothing is generated.
  2. Claude Code edits the working tree (authenticated with CLAUDE_CODE_OAUTH_TOKEN).
  3. `guard`    — checks every file Claude changed against the safety rails, reverts anything not allowed
                  and writes the Markdown report used as the PR description.

Safety rails (enforced by `guard`, also stated in the prompt):
  * Changes are only kept inside the source folders of *initialized* modules (a module is initialized when
    its build manifest exists), never in .github/, docs/, lock files or build output.
  * Dependency manifests (package.json, pubspec.yaml, build.gradle) are read-only unless ALLOW_DEPENDENCY_CHANGES=true.
  * Released Flyway migrations are immutable; new ones must be V{highest+1}__lower_snake_case.sql in backend-common.
  * Every reverted path is listed in the report so reviewers can see what the agent tried.

Local usage:
  python .github/scripts/claude_coder.py prepare --out task.md         # inspect the prompt
  python .github/scripts/claude_coder.py guard --dry-run               # check uncommitted changes, revert nothing

Environment:
  BEFORE_SHA                previous commit of the push (spec diff base; default HEAD~1)
  ALLOW_DEPENDENCY_CHANGES  true to let the agent edit package.json / pubspec.yaml / build.gradle
"""
from __future__ import annotations

import argparse
import fnmatch
import json
import os
import posixpath
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SPEC_DIR = 'docs/features'
DETAILED_SPEC = f'{SPEC_DIR}/detailed_functional_specification.md'
OVERVIEW_SPEC = f'{SPEC_DIR}/wedding_platform_feature_spec.md'
TEMPLATES_DIR = 'docs/templates'

MAX_FILE_CHARS = 200_000
# Larger spec diffs are not inlined; Claude Code reads them with `git diff` instead
MAX_INLINE_DIFF_CHARS = 60_000


# --------------------------------------------------------------------------------------
# Targets
# --------------------------------------------------------------------------------------
@dataclass(frozen=True)
class Target:
    key: str
    manifests: tuple[str, ...]      # any of these existing => module initialized
    writable: tuple[str, ...]       # glob patterns the agent may create/modify/delete (incl. the module guides)
    dependency_files: tuple[str, ...]
    guides: tuple[str, ...]         # CLAUDE.md files with the module's structure & conventions

    def initialized(self) -> bool:
        return any((REPO_ROOT / m).is_file() for m in self.manifests)


TARGETS: tuple[Target, ...] = (
    Target(
        key='web',
        manifests=('admin-console/frontend/package.json',),
        writable=('admin-console/frontend/src/*', 'admin-console/frontend/index.html', 'admin-console/frontend/CLAUDE.md'),
        dependency_files=('admin-console/frontend/package.json',),
        guides=('admin-console/frontend/CLAUDE.md',),
    ),
    Target(
        key='mobile',
        manifests=('client-console/mobile/pubspec.yaml',),
        writable=('client-console/mobile/lib/*', 'client-console/mobile/test/*', 'client-console/mobile/CLAUDE.md'),
        dependency_files=('client-console/mobile/pubspec.yaml',),
        guides=('client-console/mobile/CLAUDE.md',),
    ),
    Target(
        key='backend',
        manifests=('settings.gradle', 'settings.gradle.kts'),
        # client-console/backend is not a Gradle module yet (see its CLAUDE.md): code there would never be compiled or
        # tested, so it isn't writable until the module is set up.
        writable=('backend-common/src/*', 'admin-console/backend/src/*', 'backend-common/CLAUDE.md', 'admin-console/backend/CLAUDE.md'),
        dependency_files=(
            'build.gradle', 'build.gradle.kts', 'settings.gradle', 'settings.gradle.kts',
            'backend-common/build.gradle', 'backend-common/build.gradle.kts',
            'admin-console/backend/build.gradle', 'admin-console/backend/build.gradle.kts',
        ),
        guides=('backend-common/CLAUDE.md', 'admin-console/backend/CLAUDE.md', 'client-console/backend/CLAUDE.md'),
    ),
)
# Never writable, even if a writable pattern would match (defense in depth)
FORBIDDEN = ('.github/*', 'docs/*', '*/.env', '*/.env.*', '*.lock', '*/package-lock.json', '*/node_modules/*', '*/build/*', '*/dist/*')

# Flyway migrations: existing files are immutable, new ones must continue the version sequence
MIGRATION_DIR = 'backend-common/src/main/resources/db/migration'
MIGRATION_NAME = re.compile(r'^V(\d+)__[a-z0-9]+(?:_[a-z0-9]+)*\.sql$')


# --------------------------------------------------------------------------------------
# Helpers
# --------------------------------------------------------------------------------------
def log(msg: str) -> None:
    print(msg, flush=True)


def git(*args: str) -> str:
    # Explicit UTF-8: specs are Vietnamese and the Windows default code page can't decode them
    res = subprocess.run(['git', *args], cwd=REPO_ROOT, capture_output=True, text=True, encoding='utf-8', errors='replace', check=True)
    return res.stdout


def matches(path: str, patterns: tuple[str, ...]) -> bool:
    return any(fnmatch.fnmatchcase(path, p) for p in patterns)


def normalize_path(raw: str) -> str | None:
    """Returns a clean repo-relative POSIX path, or None if the path is unsafe."""
    p = raw.strip().replace('\\', '/')
    if not p or p.startswith('/') or (len(p) > 1 and p[1] == ':'):
        return None
    p = posixpath.normpath(p)
    if p.startswith('..') or '/../' in f'/{p}/' or p == '.':
        return None
    resolved = (REPO_ROOT / p).resolve()
    if REPO_ROOT not in resolved.parents:
        return None
    return p


def allow_dependency_changes() -> bool:
    return os.environ.get('ALLOW_DEPENDENCY_CHANGES', 'false').lower() == 'true'


def select_targets(raw: str) -> tuple[list[Target], list[Target]] | None:
    """(active, skipped) for a comma-separated --targets value; None when invalid (already logged)."""
    requested = {k.strip() for k in raw.split(',') if k.strip()}
    unknown = requested - {t.key for t in TARGETS}
    if unknown:
        log(f'❌ Unknown targets: {", ".join(sorted(unknown))}')
        return None
    selected = [t for t in TARGETS if not requested or t.key in requested]
    active = [t for t in selected if t.initialized()]
    skipped = [t for t in selected if not t.initialized()]
    for t in skipped:
        log(f'⚠️  Target "{t.key}" skipped: none of {t.manifests} exists.')
    if not active:
        log('❌ No initialized target modules to work on.')
        return None
    return active, skipped


def head_migration_versions() -> set[int]:
    """Flyway versions committed at HEAD (what is already released from this branch's point of view)."""
    try:
        names = git('ls-tree', '--name-only', 'HEAD', f'{MIGRATION_DIR}/').splitlines()
    except subprocess.CalledProcessError:
        return set()
    return {int(m.group(1)) for n in names if (m := MIGRATION_NAME.match(posixpath.basename(n)))}


# --------------------------------------------------------------------------------------
# prepare: build the task prompt
# --------------------------------------------------------------------------------------
def is_commit(ref: str) -> bool:
    try:
        git('rev-parse', '--verify', '--quiet', f'{ref}^{{commit}}')
        return True
    except subprocess.CalledProcessError:
        return False


def spec_diff_base(explicit: str | None, before_sha: str | None) -> str | None:
    """Commit the specs are compared against: an explicit --base (manual runs), else the commit before the push,
    else HEAD~1. None when no usable base exists (explicit base unknown, or first commit)."""
    if explicit:
        return explicit if is_commit(explicit) else None
    if before_sha and set(before_sha) != {'0'} and is_commit(before_sha):
        return before_sha
    # New branch, or a force-push whose previous head is no longer in the clone
    return 'HEAD~1' if is_commit('HEAD~1') else None


def spec_diff(base: str) -> str:
    """Diff of docs/features between `base` and HEAD."""
    return git('diff', '--unified=5', base, 'HEAD', '--', SPEC_DIR)


def set_output(name: str, value: str) -> None:
    """Expose a value to later workflow steps (no-op outside GitHub Actions)."""
    output = os.environ.get('GITHUB_OUTPUT')
    if output:
        with open(output, 'a', encoding='utf-8') as f:
            f.write(f'{name}={value}\n')


def build_prompt(spec_path: str, base: str, diff: str, targets: list[Target], skipped: list[Target]) -> str:
    writable = [p for t in targets for p in t.writable]
    deps = 'ALLOWED (call them out in the summary)' if allow_dependency_changes() else 'NOT allowed'
    next_migration = max(head_migration_versions(), default=0) + 1
    # Only spec *changes* are implemented (prepare stops earlier when there are none)
    if len(diff) <= MAX_INLINE_DIFF_CHARS:
        diff_section = f'What changed in the specs — implement exactly this:\n```diff\n{diff.strip()}\n```'
    else:
        diff_section = (f'The spec diff is large ({len(diff):,} chars). Read it with `git diff {base} HEAD -- {SPEC_DIR}` '
                        'and implement exactly what changed.')

    return f"""You are a senior full-stack engineer working on the Wedding Event monorepo, running unattended in CI.
Implement the product-owner spec changes in the existing code base, following its conventions exactly.

## Inputs
- Detailed functional spec (source of truth for schemas, validation rules, error codes, formulas): `{spec_path}`
- Product overview spec (context): `{OVERVIEW_SPEC}`
- UI reference templates (look & feel only): `{TEMPLATES_DIR}/`
- {diff_section}

## Scope
- Active modules: {', '.join(t.key for t in targets)}.{f" Inactive (do NOT create files for them): {', '.join(t.key for t in skipped)}." if skipped else ''}
- You may only create, modify or delete files matching: {json.dumps(writable)}.
- Never touch .github/, docs/, lock files or build output. Dependency manifests are {deps}.
- Anything outside these rules is automatically reverted after you finish, so don't rely on it.

## Module guides — read these first
Project structure, where each kind of code goes, step-by-step recipes and module rules are in `CLAUDE.md` (repo root) and:
{chr(10).join(f'- `{g}`' for t in targets for g in t.guides)}
Use them to go straight to the files you need instead of exploring the whole tree.

## Rules
1. Read the relevant spec sections and the files you will change before editing; reuse existing helpers/components.
2. Keep web, mobile and backend consistent with each other and with the spec JSON field names.
3. Write complete code — no placeholders, TODO stubs or "..." elisions. Only change code that the spec diff requires:
   don't implement other unbuilt spec items, refactor, or "fix" unrelated code. If the diff needs no code change
   (wording, typos, formatting), change nothing and say so.
4. The next Flyway migration, if the schema changes, is `{MIGRATION_DIR}/V{next_migration}__<description>.sql`.
5. Do not commit, push, create branches or open PRs — the workflow verifies the build and opens a draft PR.
6. If the spec is ambiguous or contradicts itself, choose the safest interpretation and say so.
7. If you add a new folder, pattern or shared helper, update that module's `CLAUDE.md` in the same change (keep it short).

## Final answer
End with a short Markdown summary for the PR description: what you implemented (by spec function id),
then a "### Notes" section listing spec ambiguities and anything left undone.
"""


def cmd_prepare(args: argparse.Namespace) -> int:
    spec = normalize_path(args.spec)
    if not spec or not spec.startswith(f'{SPEC_DIR}/') or not (REPO_ROOT / spec).is_file():
        log(f'❌ Spec not found under {SPEC_DIR}/: {args.spec}')
        return 1
    selection = select_targets(args.targets)
    if selection is None:
        return 1
    targets, skipped = selection

    base = spec_diff_base(args.base, os.environ.get('BEFORE_SHA'))
    if base is None:
        if args.base:
            log(f'❌ --base "{args.base}" is not a commit in this repository.')
            return 1
        log('ℹ️  No previous commit to compare the specs with — nothing to generate.')
        set_output('has_changes', 'false')
        return 0
    diff = spec_diff(base)
    if not diff.strip():
        log(f'ℹ️  No changes under {SPEC_DIR}/ between {base} and HEAD — nothing to generate.')
        set_output('has_changes', 'false')
        return 0

    prompt = build_prompt(spec, base, diff, targets, skipped)
    set_output('has_changes', 'true')
    log(f'📖 Spec: {spec} | diff: {len(diff):,} chars | targets: {", ".join(t.key for t in targets)} | prompt: {len(prompt):,} chars')
    if args.out:
        Path(args.out).write_text(prompt, encoding='utf-8')
    else:
        log(prompt)
    return 0


# --------------------------------------------------------------------------------------
# guard: validate Claude's changes, revert what's not allowed, write the report
# --------------------------------------------------------------------------------------
@dataclass
class GuardResult:
    written: list[str] = field(default_factory=list)
    deleted: list[str] = field(default_factory=list)
    rejected: list[tuple[str, str]] = field(default_factory=list)
    dependency_changes: list[str] = field(default_factory=list)


def changed_paths() -> list[tuple[str, bool, bool]]:
    """(path, existed_at_HEAD, exists_now) for every file that differs from HEAD, including untracked files."""
    tracked = [p for p in git('diff', '--name-only', '--no-renames', 'HEAD').splitlines() if p]
    untracked = [p for p in git('ls-files', '--others', '--exclude-standard').splitlines() if p]
    result = [(p, True, (REPO_ROOT / p).exists()) for p in tracked]
    result += [(p, False, True) for p in untracked]
    return sorted(set(result))


def check_change(path: str, existed: bool, exists: bool, targets: list[Target], migrations: set[int]) -> str:
    """'' when the change is allowed, otherwise the reason it is reverted."""
    if normalize_path(path) is None:
        return 'unsafe path'
    if matches(path, FORBIDDEN):
        return 'forbidden location'
    if '/db/migration/' in path and not path.startswith(MIGRATION_DIR + '/'):
        return f'Flyway migrations belong in {MIGRATION_DIR}/'
    if path.startswith(MIGRATION_DIR + '/'):
        if existed:
            return 'released Flyway migration is immutable — add a new V{n}__*.sql instead'
        name = posixpath.basename(path)
        m = MIGRATION_NAME.match(name)
        if not m:
            return f'migration name must be V{{n}}__lower_snake_case.sql, got {name}'
        expected = max(migrations, default=0) + 1
        if int(m.group(1)) != expected:
            return f'migration version must be V{expected} (next in sequence), got V{m.group(1)}'
    dep_files = tuple(f for t in targets for f in t.dependency_files)
    if path in dep_files:
        return '' if allow_dependency_changes() else 'dependency changes disabled'
    if not matches(path, tuple(p for t in targets for p in t.writable)):
        return 'outside active target source folders'
    if exists and (REPO_ROOT / path).is_file() and (REPO_ROOT / path).stat().st_size > MAX_FILE_CHARS * 4:
        return 'file too large'
    return ''


def revert(path: str, existed: bool) -> None:
    if existed:
        git('checkout', 'HEAD', '--', path)
    else:
        (REPO_ROOT / path).unlink(missing_ok=True)


def guard(targets: list[Target], dry_run: bool) -> GuardResult:
    out = GuardResult()
    migrations = head_migration_versions()

    # New migrations first, lowest version first, so several in one run are checked in sequence
    def order(change: tuple[str, bool, bool]) -> tuple[int, int, str]:
        m = MIGRATION_NAME.match(posixpath.basename(change[0]))
        return (0, int(m.group(1)), change[0]) if m and change[0].startswith(MIGRATION_DIR + '/') else (1, 0, change[0])

    for path, existed, exists in sorted(changed_paths(), key=order):
        reason = check_change(path, existed, exists, targets, migrations)
        if reason:
            out.rejected.append((path, reason))
            if not dry_run:
                revert(path, existed)
            continue
        if path.startswith(MIGRATION_DIR + '/') and (m := MIGRATION_NAME.match(posixpath.basename(path))):
            migrations.add(int(m.group(1)))
        if path in {f for t in targets for f in t.dependency_files}:
            out.dependency_changes.append(path)
        (out.written if exists else out.deleted).append(path)
    return out


def read_execution(path: str | None) -> dict:
    """Summary text, model and turn count from Claude Code's output (best effort).

    Accepts `claude -p --output-format stream-json` (one JSON event per line) as well as a single JSON
    document (`--output-format json`, or an array of events)."""
    info: dict = {}
    if not path or not Path(path).is_file():
        return info
    try:
        text = Path(path).read_text(encoding='utf-8')
    except OSError:
        return info
    try:
        data = json.loads(text)
        messages = data if isinstance(data, list) else [data]
    except json.JSONDecodeError:
        messages = []
        for line in text.splitlines():
            try:
                messages.append(json.loads(line))
            except json.JSONDecodeError:
                continue  # non-JSON noise in the log
    for msg in messages:
        if not isinstance(msg, dict):
            continue
        if msg.get('type') == 'system' and msg.get('subtype') == 'init' and msg.get('model'):
            info['model'] = msg['model']
        if msg.get('type') == 'result':
            info['summary'] = msg.get('result') or ''
            info['turns'] = msg.get('num_turns')
            info['subtype'] = msg.get('subtype')
    return info


def write_report(path: str | None, *, spec: str, targets: list[Target], skipped: list[Target], run: dict, result: GuardResult) -> None:
    agent = 'Claude Code'
    if run.get('model'):
        agent += f' (`{run["model"]}`)'
    if run.get('turns'):
        agent += f', {run["turns"]} turns'
    if run.get('subtype') not in (None, 'success'):
        agent += f' — ⚠️ ended with `{run["subtype"]}` (e.g. turn limit): the implementation may be incomplete'
    lines = [
        '## 🤖 AI-generated implementation',
        '',
        (run.get('summary') or '').strip() or '_Claude Code returned no summary._',
        '',
        '### Run details',
        f'- Spec: `{spec}`',
        f'- Agent: {agent}',
        f'- Active targets: {", ".join(t.key for t in targets) or "none"}',
    ]
    if skipped:
        lines.append(f'- ⚠️ Skipped (module not initialized): {", ".join(t.key for t in skipped)}')
    lines += ['', f'### Files changed ({len(result.written)})', *[f'- `{p}`' for p in result.written]]
    if result.deleted:
        lines += ['', '### Files deleted', *[f'- `{p}`' for p in result.deleted]]
    if result.dependency_changes:
        lines += ['', '### ⚠️ Dependency manifests changed — review carefully', *[f'- `{p}`' for p in result.dependency_changes]]
    if result.rejected:
        lines += ['', '### 🚫 Reverted by safety rails', *[f'- `{p}` — {why}' for p, why in result.rejected]]
    lines += ['', '---', '_Draft PR: verified by build/analyze/test in CI. Human review required before merge._']
    report = '\n'.join(lines) + '\n'

    if path:
        Path(path).write_text(report, encoding='utf-8')
    summary_file = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary_file:
        with open(summary_file, 'a', encoding='utf-8') as f:
            f.write(report)
    log(report)


def cmd_guard(args: argparse.Namespace) -> int:
    selection = select_targets(args.targets)
    if selection is None:
        return 1
    targets, skipped = selection
    result = guard(targets, args.dry_run)
    for p, why in result.rejected:
        log(f'🚫 {"would revert" if args.dry_run else "reverted"} {p}: {why}')
    write_report(args.report, spec=normalize_path(args.spec) or args.spec, targets=targets, skipped=skipped,
                 run=read_execution(args.execution_file), result=result)
    return 0


# --------------------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------------------
def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest='command', required=True)

    common = argparse.ArgumentParser(add_help=False)
    common.add_argument('--spec', default=DETAILED_SPEC, help='spec file to implement (repo-relative)')
    common.add_argument('--targets', default='', help='comma-separated subset of: ' + ','.join(t.key for t in TARGETS))

    p = sub.add_parser('prepare', parents=[common], help='write the task prompt for Claude Code')
    p.add_argument('--out', help='write the prompt to this file (default: print it)')
    p.add_argument('--base', help='compare the specs against this commit/tag/branch (default: BEFORE_SHA, else HEAD~1)')
    p.set_defaults(func=cmd_prepare)

    g = sub.add_parser('guard', parents=[common], help='validate/revert Claude\'s changes and write the PR report')
    g.add_argument('--report', help='write the Markdown report (PR body) to this path')
    g.add_argument('--execution-file', help='output of `claude -p --output-format stream-json` (for the PR summary)')
    g.add_argument('--dry-run', action='store_true', help='report what would be reverted without reverting')
    g.set_defaults(func=cmd_guard)

    args = parser.parse_args()
    return args.func(args)


if __name__ == '__main__':
    sys.exit(main())
