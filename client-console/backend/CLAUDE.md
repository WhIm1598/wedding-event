# client-console/backend — WedPlanner API (not implemented yet)

Planned Spring Boot 4 service for the mobile app: `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/client/**`,
`/api/v1/messages/**` and WebSocket `/ws/**` (routing: spec §1.4). Endpoints are specified in **Part A** of
`docs/features/detailed_functional_specification.md`; the mobile repositories in `client-console/mobile/lib/data/repositories`
already call them, so match their paths and JSON exactly.

## Status
There is no source and no build file here yet, and the module is not included in `settings.gradle.kts`
(see the TODO there). Code written under `src/` is **not compiled or tested** until the module is set up.
Don't generate code for this module unless the task explicitly asks to create it.

## When creating it
- Mirror `admin-console/backend`: `build.gradle.kts` depending on `project(":backend-common")`, package
  `com.weddingevent.client`, one package per spec module, same `ApiResponse` envelope, `AppException` errors,
  integration tests on PostgreSQL. Follow [admin-console/backend/CLAUDE.md](../../admin-console/backend/CLAUDE.md).
- Port 8081 (the mobile default `API_BASE_URL` is `http://10.0.2.2:8081/api/v1`).
- New tables (wedding plans, guests, chat, …) go into backend-common: entities + Flyway migrations there
  (see [backend-common/CLAUDE.md](../../backend-common/CLAUDE.md)). Both services apply the same migration set.
- Add `include(":client-console:backend")` to `settings.gradle.kts` (a dependency/build change — needs human approval).
