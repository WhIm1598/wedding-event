# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Wedding Event monorepo

Two products share one PostgreSQL database and one Java domain library:
- **Lumière Studios admin** (web, studio staff): `admin-console/frontend` → `admin-console/backend`
- **WedPlanner** (mobile, couples & vendors): `client-console/mobile` → `client-console/backend`

| Module | Stack | Guide |
|---|---|---|
| `backend-common/` | Java 21 library: JPA entities, enums, repositories, API envelope, errors, **Flyway migrations** | [backend-common/CLAUDE.md](backend-common/CLAUDE.md) |
| `admin-console/backend/` | Spring Boot 4 API, port 8082, `/api/v1/admin/**`, `/api/v1/public/**` | [admin-console/backend/CLAUDE.md](admin-console/backend/CLAUDE.md) |
| `admin-console/frontend/` | React 18 + TS + Vite + Tailwind, mock or real API | [admin-console/frontend/CLAUDE.md](admin-console/frontend/CLAUDE.md) |
| `client-console/mobile/` | Flutter app, mock or real API | [client-console/mobile/CLAUDE.md](client-console/mobile/CLAUDE.md) |
| `client-console/backend/` | Spring Boot 4 API, port 8081, `/api/v1/auth\|users\|client\|messages/**`, STOMP `/ws` | [client-console/backend/CLAUDE.md](client-console/backend/CLAUDE.md) |
| `docs/features/` | Specs — **source of truth** | `detailed_functional_specification.md` (contracts), `wedding_platform_feature_spec.md` (product) |
| `docs/architecture/` | Infra, nginx routing, Docker | `system_architecture_and_infrastructure.md` |
| `docs/api/` | OpenAPI 3.1 contract of what the backends actually serve (`admin-api.yml` for the admin API, `client-api.yml` for the mobile API); maintained with the `api-documenter` agent | |
| `docs/templates/` | UI reference mock-ups only (look & feel, not code to copy) | |
| `.github/` | CI (`ci.yml`), spec-to-code agent (`claude-agent.yml`, `scripts/claude_coder.py`) | |
| `.claude/agents/` | Project subagents: `backend-architect` (read-only design proposal before backend work), `java-architect` (backend Java/Spring implementation + code-level design), `frontend-developer` (admin web implementation), `api-documenter` (OpenAPI `docs/api/*.yml` from the code), `code-reviewer` (read-only review; also runs in CI after code generation) | |

## How a feature flows
Spec function (e.g. `FN-ADM-CAL-02`) → entity/enum/migration in `backend-common` → DTO + service + controller + security rule
in the backend → TS type + API function (real **and** mock branch) + screen in the frontend (or Dart model + repository + screen
on mobile). JSON field names are identical at every layer; the spec's names win.

## Ground rules
- The detailed spec defines endpoints, schemas, validation, error codes and formulas. Functions are tagged ✅ built,
  ⚠️ built but changing, 🆕 not built; `(v1.1)` marks planned parts. Don't invent endpoints or fields outside it.
- The schema changes **only** through a new Flyway migration (`backend-common/src/main/resources/db/migration`). Never edit a released one.
- Error responses use the envelope `{success, code, message, errors}` with the spec's error codes; user-facing text is Vietnamese.
- Money is integer VND, dates `YYYY-MM-DD`, times `HH:mm`, business time zone `Asia/Ho_Chi_Minh`.
- Don't change dependency manifests (`package.json`, `pubspec.yaml`, `*.gradle.kts`) or lock files unless asked.
- Keep demo/mock modes working: web `npm run dev` and mobile `USE_MOCK=true` run without any backend.
- When you add a new folder or pattern, update the module's `CLAUDE.md` in the same change.
- Backend work is **test-driven with JUnit 5**: tests from the spec/design first, then code. Coverage gate: 85% lines.

## Commands (repo root unless noted)
| Check | Command |
|---|---|
| Backend build + tests | `./gradlew build` (Testcontainers needs Docker; or set `TEST_DATABASE_URL` to an empty PostgreSQL). Fails below **85% line coverage** (JaCoCo); report at `<module>/build/reports/jacoco/test/html/index.html` |
| One module / one test | `./gradlew :admin-console:backend:test` · `./gradlew :client-console:backend:test --tests '*PlanApiTest'` (modules: `:backend-common`, `:admin-console:backend`, `:client-console:backend`) |
| Run admin API | `docker compose -f docker-compose.dev.yml up -d` then `./gradlew :admin-console:backend:bootRun --args='--spring.profiles.active=dev'` (no default profile) |
| Run mobile API | same database, then `./gradlew :client-console:backend:bootRun` (port 8081) |
| Web type-check + build | `cd admin-console/frontend && npm run build` (type-check only: `npm run typecheck`; the web app has no test runner or linter) |
| Web dev (mock / real API) | `npm run dev` / `npm run dev:api` |
| Mobile | `cd client-console/mobile && flutter analyze && flutter test` (one file: `flutter test test/<file>_test.dart`; real API: `flutter run --dart-define=USE_MOCK=false`) |

CI (`.github/actions/verify-monorepo`) runs the same checks, plus a job rejecting edits to released migrations.

Spec-to-code agent locally: `python .github/scripts/claude_coder.py prepare --targets web --out task.md`, then
`guard --dry-run --targets web`. **`guard` without `--dry-run` reverts every uncommitted change outside the allowed scope**, so only run it on a clean tree.

## Agent skills

### Issue tracker

Issues live in GitHub Issues for `WhIm1598/wedding-event` (via `gh`). See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: root `GLOSSARY.md` + `docs/adr/`. See `docs/agents/domain.md`.
