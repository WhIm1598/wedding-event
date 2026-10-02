# Wedding Event monorepo — guide for Claude

Two products share one PostgreSQL database and one Java domain library:
- **Lumière Studios admin** (web, studio staff): `admin-console/frontend` → `admin-console/backend`
- **WedPlanner** (mobile, couples & vendors): `client-console/mobile` → `client-console/backend` (not built yet)

| Module | Stack | Guide |
|---|---|---|
| `backend-common/` | Java 21 library: JPA entities, enums, repositories, API envelope, errors, **Flyway migrations** | [backend-common/CLAUDE.md](backend-common/CLAUDE.md) |
| `admin-console/backend/` | Spring Boot 4 API, port 8082, `/api/v1/admin/**`, `/api/v1/public/**` | [admin-console/backend/CLAUDE.md](admin-console/backend/CLAUDE.md) |
| `admin-console/frontend/` | React 18 + TS + Vite + Tailwind, mock or real API | [admin-console/frontend/CLAUDE.md](admin-console/frontend/CLAUDE.md) |
| `client-console/mobile/` | Flutter app, mock or real API | [client-console/mobile/CLAUDE.md](client-console/mobile/CLAUDE.md) |
| `client-console/backend/` | Planned Spring Boot API `/api/v1/client/**`, `/api/v1/auth/**` | [client-console/backend/CLAUDE.md](client-console/backend/CLAUDE.md) |
| `docs/features/` | Specs — **source of truth** | `detailed_functional_specification.md` (contracts), `wedding_platform_feature_spec.md` (product) |
| `docs/architecture/` | Infra, nginx routing, Docker | `system_architecture_and_infrastructure.md` |
| `docs/templates/` | UI reference mock-ups only (look & feel, not code to copy) | |
| `.github/` | CI (`ci.yml`), spec-to-code agent (`claude-agent.yml`, `scripts/claude_coder.py`) | |

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

## Commands (repo root unless noted)
| Check | Command |
|---|---|
| Backend build + tests | `./gradlew build` (Testcontainers needs Docker; or set `TEST_DATABASE_URL` to an empty PostgreSQL) |
| Run admin API | `docker compose -f docker-compose.dev.yml up -d` then `./gradlew :admin-console:backend:bootRun` |
| Web type-check + build | `cd admin-console/frontend && npm run build` |
| Web dev (mock / real API) | `npm run dev` / `npm run dev:api` |
| Mobile | `cd client-console/mobile && flutter analyze && flutter test` |

CI (`.github/actions/verify-monorepo`) runs the same checks, plus a job rejecting edits to released migrations.
