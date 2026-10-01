#!/usr/bin/env python3
"""
Spec-to-code agent: reads the PO specs in docs/features/, sends them together with the
current module source to Claude, and writes the returned files back into the repo.

Safety rails:
  * Writes/deletes are only allowed inside the source folders of *initialized* modules
    (a module is initialized when its build manifest exists), never in .github/ or docs/.
  * Dependency manifests (package.json, pubspec.yaml, build.gradle) are read-only unless
    ALLOW_DEPENDENCY_CHANGES=true.
  * Every rejected path is listed in the report so reviewers can see what the model tried.

Local usage:
  python .github/scripts/claude_coder.py --print-prompt          # inspect prompt, no API call
  ANTHROPIC_API_KEY=... python .github/scripts/claude_coder.py --dry-run

Environment:
  ANTHROPIC_API_KEY         required for real runs
  CLAUDE_MODEL              default claude-opus-5-5
  CLAUDE_EFFORT             low | medium | high | xhigh | max (default high)
  ALLOW_DEPENDENCY_CHANGES  true to let the model edit package.json / pubspec.yaml / build.gradle
  CONTEXT_CHAR_BUDGET       max characters of source sent as context (default 700000)
"""
from __future__ import annotations

import argparse
import fnmatch
import json
import os
import posixpath
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SPEC_DIR = 'docs/features'
DETAILED_SPEC = f'{SPEC_DIR}/detailed_functional_specification.md'
OVERVIEW_SPEC = f'{SPEC_DIR}/wedding_platform_feature_spec.md'
TEMPLATES_DIR = 'docs/templates'

DEFAULT_MODEL = 'claude-opus-5-5'
DEFAULT_EFFORT = 'high'
EFFORT_LEVELS = ('low', 'medium', 'high', 'xhigh', 'max')
CONTEXT_CHAR_BUDGET = int(os.environ.get('CONTEXT_CHAR_BUDGET', '700000'))
MAX_FILE_CHARS = 200_000
MAX_OUTPUT_TOKENS = 128_000  # model maximum; streaming keeps long generations under HTTP timeouts


# --------------------------------------------------------------------------------------
# Targets
# --------------------------------------------------------------------------------------
@dataclass(frozen=True)
class Target:
    key: str
    roots: tuple[str, ...]          # folders scanned for context
    manifests: tuple[str, ...]      # any of these existing => module initialized
    writable: tuple[str, ...]       # glob patterns the agent may create/modify/delete
    dependency_files: tuple[str, ...]
    conventions: str

    def initialized(self) -> bool:
        return any((REPO_ROOT / m).is_file() for m in self.manifests)


TARGETS: tuple[Target, ...] = (
    Target(
        key='web',
        roots=('admin-console/frontend',),
        manifests=('admin-console/frontend/package.json',),
        writable=('admin-console/frontend/src/*', 'admin-console/frontend/index.html'),
        dependency_files=('admin-console/frontend/package.json',),
        conventions="""\
admin-console/frontend — React 18 + TypeScript (strict) + Vite + Tailwind CSS 3 + React Router 6 + lucide-react.
- Domain types live in src/types/index.ts and mirror the detailed spec (enums as string unions, money = number VND, dates = ISO strings).
- One API module per spec module in src/api/<module>.api.ts. EVERY function has two branches:
  `if (!env.useMock) return http.<verb>('/admin/...')` (real endpoint from the spec) and a mock branch that reads/mutates
  the in-memory db in src/api/mock/db.ts and returns `delay(...)`. Keep mock mode working: add seed data for new entities.
- Business errors from the spec are thrown as `new ApiError(status, CODE, message)` in mock mode too.
- Screens live in src/features/<module>/ (page + components/). Shared UI: src/components/ui (Badge, Button, Card, Field/Input/Select/Textarea, Modal/SlideOver, PageHeader, Spinner/ErrorState/EmptyState).
- Enum -> Vietnamese label/badge colour mappings go in src/constants/labels.ts. Formatting helpers: src/lib/format.ts, src/lib/date.ts.
- New pages: add a route in src/app/router.tsx (wrap admin-only pages with adminOnly) and a menu entry in src/app/navigation.ts.
- Data loading via the useAsync hook; forms read values with FormData; feedback via useToast(). UI text is Vietnamese.
- Code must pass `tsc --noEmit` with noUnusedLocals/noUnusedParameters.""",
    ),
    Target(
        key='mobile',
        roots=('client-console/mobile',),
        manifests=('client-console/mobile/pubspec.yaml',),
        writable=('client-console/mobile/lib/*', 'client-console/mobile/test/*'),
        dependency_files=('client-console/mobile/pubspec.yaml',),
        conventions="""\
client-console/mobile — Flutter (Dart 3.5+), go_router, provider, dio. Package name: wedplanner.
- Models in lib/data/models (immutable, const constructors, fromJson/toJson matching the spec JSON).
- One repository per API module in lib/data/repositories. EVERY method branches on `Env.useMock`:
  mock branch uses lib/data/mock/mock_data.dart + `mockDelay(...)`, real branch calls `_api.get/post/put/patch(path, parse: ...)`
  against the client backend (/client/**, /auth/**). Spec error codes are thrown as ApiException(code:, message:).
- Repositories are provided in lib/app.dart (MultiProvider); session/role state is SessionController (lib/state).
- Screens in lib/features/<feature>/; register new routes in lib/core/router/app_router.dart (Routes constants).
- Use AppColors / shared widgets from lib/core/widgets/common.dart (AppCard, ScreenHeader, TabHeader, PrimaryButton, AsyncView, StatusChip, IconBubble, showAppSnack).
  Money/date formatting via Fmt, validation via Validators (lib/core/utils). UI text is Vietnamese.
- Must pass `flutter analyze` (flutter_lints) with zero issues: do NOT use deprecated APIs (Color.withOpacity, MaterialStateProperty,
  DropdownButtonFormField(value:), Switch(activeColor:)); check `mounted` after every await before using context;
  never call context.read inside build (use context.watch); always pass super.key; dispose controllers.
- Add/extend unit tests in test/ for new pure logic (formatters, validators, calculations).""",
    ),
    Target(
        key='backend',
        roots=('backend-common', 'admin-console/backend', 'client-console/backend'),
        manifests=('settings.gradle', 'settings.gradle.kts'),
        writable=('backend-common/src/*', 'admin-console/backend/src/*', 'client-console/backend/src/*'),
        dependency_files=(
            'build.gradle', 'build.gradle.kts', 'settings.gradle', 'settings.gradle.kts',
            'backend-common/build.gradle', 'backend-common/build.gradle.kts',
            'admin-console/backend/build.gradle', 'admin-console/backend/build.gradle.kts',
            'client-console/backend/build.gradle', 'client-console/backend/build.gradle.kts',
        ),
        conventions="""\
Backend — Java 21, Spring Boot 4, jakarta.* (Persistence, Validation), Gradle multi-module.
- backend-common: JPA entities, enums, repositories, ApiResponse<T> envelope {success, code, message, data, errors}, AppException hierarchy.
- admin-console/backend: controllers under /api/v1/admin/** (ROLE_ADMIN / ROLE_STAFF). client-console/backend: /api/v1/client/** and /api/v1/auth/**.
- Request DTOs carry Bean Validation annotations matching the spec's validation rules; error codes exactly as in the spec.
- Response JSON field names must match the TypeScript/Dart models already used by the web and mobile apps.""",
    ),
)

# Never writable, even if a writable pattern would match (defense in depth)
FORBIDDEN = ('.github/*', 'docs/*', '*/.env', '*/.env.*', '*.lock', '*/package-lock.json', '*/node_modules/*', '*/build/*', '*/dist/*')

CONTEXT_EXTENSIONS = {'.ts', '.tsx', '.js', '.json', '.html', '.css', '.dart', '.yaml', '.yml', '.java', '.gradle', '.kts', '.properties', '.xml'}
CONTEXT_SKIP_DIRS = {'node_modules', 'dist', 'build', '.dart_tool', '.git', '.gradle', '.idea', '.vscode', 'ios', 'android', '__pycache__', '.antigravity'}
CONTEXT_SKIP_FILES = {'package-lock.json', 'pubspec.lock'}

# Contract-like files are sent first so they survive the context budget
PRIORITY_HINTS = ('types/', '/models/', '/api/', '/repositories/', 'router', 'navigation', '/config/', '/theme/', 'labels', 'mock', 'app.dart', 'main.')


# --------------------------------------------------------------------------------------
# Helpers
# --------------------------------------------------------------------------------------
def log(msg: str) -> None:
    print(msg, flush=True)


def git(*args: str) -> str:
    res = subprocess.run(['git', *args], cwd=REPO_ROOT, capture_output=True, text=True, check=True)
    return res.stdout


def read(rel: str) -> str:
    return (REPO_ROOT / rel).read_text(encoding='utf-8')


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


# --------------------------------------------------------------------------------------
# Spec & context
# --------------------------------------------------------------------------------------
def spec_diff(before_sha: str | None) -> str:
    """Diff of docs/features between the previous push state and HEAD (empty for manual runs)."""
    base = before_sha if before_sha and set(before_sha) != {'0'} else 'HEAD~1'
    try:
        return git('diff', '--unified=5', base, 'HEAD', '--', SPEC_DIR)
    except subprocess.CalledProcessError:
        return ''


def gather_context(targets: list[Target]) -> tuple[list[dict[str, str]], list[str]]:
    candidates: list[str] = []
    for target in targets:
        for root in target.roots:
            base = REPO_ROOT / root
            if not base.is_dir():
                continue
            for path in base.rglob('*'):
                rel_parts = path.relative_to(REPO_ROOT).parts
                if not path.is_file() or CONTEXT_SKIP_DIRS.intersection(rel_parts):
                    continue
                if path.suffix not in CONTEXT_EXTENSIONS or path.name in CONTEXT_SKIP_FILES:
                    continue
                candidates.append(path.relative_to(REPO_ROOT).as_posix())

    # UI templates are reference material, lowest priority
    templates = sorted(p.relative_to(REPO_ROOT).as_posix() for p in (REPO_ROOT / TEMPLATES_DIR).glob('*') if p.is_file())

    def priority(rel: str) -> tuple[int, str]:
        return (0 if any(h in rel for h in PRIORITY_HINTS) else 1, rel)

    ordered = sorted(set(candidates), key=priority) + templates
    included: list[dict[str, str]] = []
    omitted: list[str] = []
    used = 0
    for rel in ordered:
        try:
            content = read(rel)
        except (UnicodeDecodeError, OSError):
            continue
        if used + len(content) > CONTEXT_CHAR_BUDGET:
            omitted.append(rel)
            continue
        included.append({'path': rel, 'content': content})
        used += len(content)
    return included, omitted


def build_prompt(spec_path: str, diff: str, targets: list[Target], skipped: list[Target], context: list[dict[str, str]], omitted: list[str]) -> tuple[str, str]:
    writable = [p for t in targets for p in t.writable]
    system = f"""You are a senior full-stack engineer working on the Wedding Event monorepo.
Your job: implement the product-owner spec changes in the existing code base, following its conventions exactly.

ACTIVE TARGETS (you may only modify files matching these patterns): {json.dumps(writable)}
{"INACTIVE TARGETS (do NOT generate files for them, module not initialized): " + ", ".join(t.key for t in skipped) if skipped else ""}
Never touch .github/, docs/, lock files or dependency manifests ({'allowed' if allow_dependency_changes() else 'NOT allowed'} to change dependencies).

MODULE CONVENTIONS
{chr(10).join(t.conventions for t in targets)}

RULES
1. The detailed functional spec is the source of truth for schemas, validation rules, error codes and formulas;
   the overview spec gives product context. The "SPEC DIFF" shows what just changed — focus on it.
2. Keep web and mobile consistent with each other and with the spec JSON field names.
3. Return ONLY files you create or change, each with its COMPLETE final content (no placeholders, no "..." elisions).
   List files to remove in `deletions`. Do not return unchanged files.
4. Keep changes minimal and focused; reuse existing helpers/components instead of duplicating them.
5. If the spec is ambiguous or contradicts itself, choose the safest interpretation and explain it in `notes`.
6. `summary` is a short Markdown description of what you implemented (used as the PR description).
"""
    prompt = f"""SPEC DIFF (what changed since the previous version; empty = implement anything in the spec not yet implemented):
```diff
{diff.strip() or '(no diff available)'}
```

DETAILED FUNCTIONAL SPEC ({spec_path}):
---
{read(spec_path)}
---

OVERVIEW SPEC ({OVERVIEW_SPEC}):
---
{read(OVERVIEW_SPEC) if spec_path != OVERVIEW_SPEC else '(same as above)'}
---

CURRENT SOURCE ({len(context)} files, JSON array of {{path, content}}):
{json.dumps(context, ensure_ascii=False)}

FILES THAT EXIST BUT WERE OMITTED FOR SIZE (do not recreate them; ask for them in notes if needed):
{json.dumps(omitted)}
"""
    return system, prompt


# Structured output schema (output_config.format): every object needs additionalProperties=false
RESPONSE_SCHEMA = {
    'type': 'object',
    'properties': {
        'summary': {'type': 'string'},
        'files': {
            'type': 'array',
            'items': {
                'type': 'object',
                'properties': {'path': {'type': 'string'}, 'content': {'type': 'string'}},
                'required': ['path', 'content'],
                'additionalProperties': False,
            },
        },
        'deletions': {'type': 'array', 'items': {'type': 'string'}},
        'notes': {'type': 'array', 'items': {'type': 'string'}},
    },
    'required': ['summary', 'files', 'deletions', 'notes'],
    'additionalProperties': False,
}


def allow_dependency_changes() -> bool:
    return os.environ.get('ALLOW_DEPENDENCY_CHANGES', 'false').lower() == 'true'


# --------------------------------------------------------------------------------------
# Claude call
# --------------------------------------------------------------------------------------
@dataclass
class CallInfo:
    requested_model: str
    served_model: str
    fallback_used: bool
    input_tokens: int
    output_tokens: int
    request_id: str | None


def call_claude(system: str, prompt: str, model: str, effort: str) -> tuple[dict, CallInfo]:
    import anthropic  # imported lazily so --print-prompt works without the SDK

    # The SDK retries 408/409/429/5xx and connection errors on the initial request (max_retries).
    # Generations here can run for many minutes, so a long timeout + streaming is required.
    log(f'🤖 Calling {model} (effort={effort}, max_tokens={MAX_OUTPUT_TOKENS})...')
    try:
        client = anthropic.Anthropic(max_retries=4, timeout=60 * 60)
        with client.beta.messages.stream(
            model=model,
            max_tokens=MAX_OUTPUT_TOKENS,
            system=system,
            messages=[{'role': 'user', 'content': prompt}],
            # Thinking is always on for Opus 5.5; effort controls depth (its default is medium).
            output_config={
                'effort': effort,
                'format': {'type': 'json_schema', 'schema': RESPONSE_SCHEMA},
            },
            # On a safety-classifier decline, re-run on Anthropic's recommended fallback model
            # instead of failing the CI run.
            betas=['server-side-fallback-2026-07-01'],
            fallbacks='default',
        ) as stream:
            response = stream.get_final_message()
            request_id = stream.request_id  # log it when reporting failures to Anthropic
    except anthropic.AuthenticationError as e:
        raise RuntimeError('Invalid ANTHROPIC_API_KEY.') from e
    except anthropic.PermissionDeniedError as e:
        raise RuntimeError(f'API key lacks permission for {model}: {e.message}') from e
    except anthropic.NotFoundError as e:
        raise RuntimeError(f'Unknown model "{model}" — check the CLAUDE_MODEL variable.') from e
    except anthropic.BadRequestError as e:
        raise RuntimeError(f'Request rejected (400): {e.message}') from e
    except anthropic.RateLimitError as e:
        raise RuntimeError(f'Rate limited after retries (request-id: {e.request_id}). Re-run the workflow later.') from e
    except anthropic.APIStatusError as e:
        raise RuntimeError(f'API error {e.status_code} after retries (request-id: {e.request_id}): {e.message}') from e
    except anthropic.APIConnectionError as e:
        raise RuntimeError(f'Connection to the Claude API failed: {e}') from e
    except anthropic.AnthropicError as e:  # e.g. no credentials configured
        raise RuntimeError(f'Claude client error: {e}') from e

    if response.stop_reason == 'refusal':
        details = response.stop_details
        category = getattr(details, 'category', None) if details else None
        raise RuntimeError(f'Model declined the request (category: {category}). Review the spec content.')
    if response.stop_reason == 'max_tokens':
        raise RuntimeError(
            f'Response truncated at {MAX_OUTPUT_TOKENS} output tokens. '
            'Split the spec change into smaller commits or run with fewer --targets.'
        )

    # output_config.format guarantees the text block is JSON matching RESPONSE_SCHEMA
    text = next((b.text for b in response.content if b.type == 'text'), '')
    try:
        result = json.loads(text)
    except json.JSONDecodeError as e:
        raise RuntimeError(f'Could not parse model output as JSON: {e}') from e

    fallback_used = any(getattr(it, 'type', None) == 'fallback_message' for it in (response.usage.iterations or []))
    info = CallInfo(
        requested_model=model,
        served_model=response.model,
        fallback_used=fallback_used,
        input_tokens=response.usage.input_tokens,
        output_tokens=response.usage.output_tokens,
        request_id=request_id,
    )
    log(f'✅ {info.served_model} | in={info.input_tokens} out={info.output_tokens} tokens | request-id={info.request_id}')
    return result, info


# --------------------------------------------------------------------------------------
# Apply result
# --------------------------------------------------------------------------------------
@dataclass
class ApplyResult:
    written: list[str] = field(default_factory=list)
    deleted: list[str] = field(default_factory=list)
    rejected: list[tuple[str, str]] = field(default_factory=list)
    dependency_changes: list[str] = field(default_factory=list)


def check_path(raw: str, targets: list[Target]) -> tuple[str | None, str]:
    path = normalize_path(raw)
    if path is None:
        return None, 'unsafe path'
    if matches(path, FORBIDDEN):
        return None, 'forbidden location'
    dep_files = tuple(f for t in targets for f in t.dependency_files)
    if path in dep_files:
        return (path, 'dependency manifest') if allow_dependency_changes() else (None, 'dependency changes disabled')
    if not matches(path, tuple(p for t in targets for p in t.writable)):
        return None, 'outside active target source folders'
    return path, ''


def apply_result(result: dict, targets: list[Target], dry_run: bool) -> ApplyResult:
    out = ApplyResult()
    for item in result.get('files', []):
        path, reason = check_path(item.get('path', ''), targets)
        content = item.get('content', '')
        if path is None:
            out.rejected.append((item.get('path', ''), reason))
            continue
        if len(content) > MAX_FILE_CHARS:
            out.rejected.append((path, f'file too large ({len(content)} chars)'))
            continue
        if reason == 'dependency manifest':
            out.dependency_changes.append(path)
        if not dry_run:
            dest = REPO_ROOT / path
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(content if content.endswith('\n') else content + '\n', encoding='utf-8')
        out.written.append(path)

    for raw in result.get('deletions', []) or []:
        path, reason = check_path(raw, targets)
        if path is None or reason == 'dependency manifest':
            out.rejected.append((raw, f'delete refused: {reason or "dependency manifest"}'))
            continue
        if (REPO_ROOT / path).is_file():
            if not dry_run:
                (REPO_ROOT / path).unlink()
            out.deleted.append(path)
    return out


def write_report(path: str | None, *, spec: str, call: CallInfo, effort: str, targets: list[Target], skipped: list[Target], result: dict, applied: ApplyResult, omitted: list[str]) -> None:
    model_line = f'- Model: `{call.served_model}` (effort `{effort}`)'
    if call.fallback_used:
        model_line += f' — ⚠️ fallback from `{call.requested_model}` after a safety decline'
    lines = [
        '## 🤖 AI-generated implementation',
        '',
        result.get('summary', '').strip() or '_No summary returned._',
        '',
        '### Run details',
        f'- Spec: `{spec}`',
        model_line,
        f'- Tokens: {call.input_tokens:,} in / {call.output_tokens:,} out (request-id `{call.request_id}`)',
        f'- Active targets: {", ".join(t.key for t in targets) or "none"}',
    ]
    if skipped:
        lines.append(f'- ⚠️ Skipped (module not initialized): {", ".join(t.key for t in skipped)}')
    if omitted:
        lines.append(f'- ⚠️ {len(omitted)} file(s) omitted from model context due to size budget')
    lines += ['', f'### Files written ({len(applied.written)})', *[f'- `{p}`' for p in applied.written]]
    if applied.deleted:
        lines += ['', '### Files deleted', *[f'- `{p}`' for p in applied.deleted]]
    if applied.dependency_changes:
        lines += ['', '### ⚠️ Dependency manifests changed — review carefully', *[f'- `{p}`' for p in applied.dependency_changes]]
    if applied.rejected:
        lines += ['', '### 🚫 Rejected by safety rails', *[f'- `{p}` — {why}' for p, why in applied.rejected]]
    notes = result.get('notes') or []
    if notes:
        lines += ['', '### Model notes / spec ambiguities', *[f'- {n}' for n in notes]]
    lines += ['', '---', '_Draft PR: verified by build/analyze/test in CI. Human review required before merge._']
    report = '\n'.join(lines) + '\n'

    if path:
        Path(path).write_text(report, encoding='utf-8')
    summary_file = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary_file:
        with open(summary_file, 'a', encoding='utf-8') as f:
            f.write(report)
    log(report)


# --------------------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------------------
def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--spec', default=DETAILED_SPEC, help='spec file to implement (repo-relative)')
    parser.add_argument('--targets', default='', help='comma-separated subset of: ' + ','.join(t.key for t in TARGETS))
    parser.add_argument('--report', help='write the Markdown report (PR body) to this path')
    parser.add_argument('--dry-run', action='store_true', help='call the model but do not write files')
    parser.add_argument('--print-prompt', action='store_true', help='print prompt statistics and exit (no API call)')
    args = parser.parse_args()

    spec = normalize_path(args.spec)
    if not spec or not spec.startswith(f'{SPEC_DIR}/') or not (REPO_ROOT / spec).is_file():
        log(f'❌ Spec not found under {SPEC_DIR}/: {args.spec}')
        return 1

    requested = {k.strip() for k in args.targets.split(',') if k.strip()}
    unknown = requested - {t.key for t in TARGETS}
    if unknown:
        log(f'❌ Unknown targets: {", ".join(sorted(unknown))}')
        return 1
    selected = [t for t in TARGETS if not requested or t.key in requested]
    targets = [t for t in selected if t.initialized()]
    skipped = [t for t in selected if not t.initialized()]
    for t in skipped:
        log(f'⚠️  Target "{t.key}" skipped: none of {t.manifests} exists.')
    if not targets:
        log('❌ No initialized target modules to work on.')
        return 1

    diff = spec_diff(os.environ.get('BEFORE_SHA'))
    context, omitted = gather_context(targets)
    system, prompt = build_prompt(spec, diff, targets, skipped, context, omitted)
    log(f'📖 Spec: {spec} | diff: {len(diff)} chars | context: {len(context)} files, {len(prompt)} chars | omitted: {len(omitted)}')

    if args.print_prompt:
        for f in context:
            log(f'   - {f["path"]} ({len(f["content"])} chars)')
        return 0

    model = os.environ.get('CLAUDE_MODEL') or DEFAULT_MODEL
    effort = (os.environ.get('CLAUDE_EFFORT') or DEFAULT_EFFORT).lower()
    if effort not in EFFORT_LEVELS:
        log(f'❌ CLAUDE_EFFORT must be one of {", ".join(EFFORT_LEVELS)} (got "{effort}").')
        return 1

    try:
        result, call = call_claude(system, prompt, model, effort)
    except RuntimeError as e:
        log(f'❌ {e}')
        return 1

    applied = apply_result(result, targets, args.dry_run)
    write_report(args.report, spec=spec, call=call, effort=effort, targets=targets, skipped=skipped, result=result, applied=applied, omitted=omitted)
    return 0


if __name__ == '__main__':
    sys.exit(main())
