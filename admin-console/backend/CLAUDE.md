# admin-console/backend — Lumière Studios admin API

Spring Boot 4.1 · Java 21 · Spring Security 7 (stateless JWT HS256) · Spring Data JPA / Hibernate 7 · Flyway · PostgreSQL 16.
Port 8082. Every route is under `/api/v1/admin/**` (nginx forwards only that prefix here, plus `/api/v1/public/**`).
Entities, enums, repositories and migrations live in **backend-common**, not here.

## Layout (`src/main/java/com/weddingevent/admin/`)
One package per spec module: `auth`, `dashboard`, `booking`, `packages`, `asset`, `staff`, `crm`, `contract`, `finance`,
`notification`, `search`, `settings` (+ `ZaloOaSettingsController`), `user` (FN-ADM-USER-01 login accounts), `platform` (FN-PLT-01..03, `/api/v1/platform/**`, ROLE_PLATFORM_ADMIN). Shared code:
| Package | Contents |
|---|---|
| `security/` | `SecurityConfig` (URL rules, JWT encoder/decoder), `TokenService` (refresh tokens are `refresh_tokens` rows keyed by `jti`, rotated in `AuthService.refresh`), `ConsoleAuthenticationConverter` (reloads the account on every request: locked / role or staff link changed → 401; a temporary password grants only `PASSWORD_CHANGE_REQUIRED`, so routes not opened with `.authenticated()` answer 403 `PASSWORD_CHANGE_REQUIRED`), `CurrentUser.from(jwt)` (`id`, `role`, `staffMemberId`, `isAdmin()`), `SecurityErrorHandler` |
| `config/` | `AppProperties` (`app.*`: zone, jwt, cors, demo-data, `public-base-url` for customer links, `zalo` endpoint URLs), `TimeConfig` (`Clock` in Asia/Ho_Chi_Minh) |
| `zalo/` | `ZaloOaClient` (OAuth v4 token refresh + ZNS send; Zalo answers 200 with `error ≠ 0`), `ZaloOaTokens.accessToken()` (the studio's OA token, refreshed in a `REQUIRES_NEW` transaction under the settings row lock — the refresh token is single-use). Tests fake Zalo with `ZaloStub` (`IntegrationTest.ZALO`) |
| `contract/ContractShareService` + `PublicContractController` | Zalo sharing and the public `/api/v1/public/contracts/{token}` page (only the token's SHA-256 is stored) |
| `support/` | `DateRange` (day / Monday-based week / month, instant bounds) |
| `notification/` | `NotificationService.notify(title, description)` — call it for events the team should see |
| `crm/CustomerStageSync` | `contractChanged(contract)` — call after any contract status change (payment, delivery) so the customer's stage follows (FN-ADM-CRM-03) |
| `demo/` | `DemoDataSeeder` — seeds the empty dev/test database (profile flag `app.demo-data.enabled`). There is no default profile: run with `--spring.profiles.active=dev` |
| `auth/AdminBootstrap` | Production's platform admin from env `ADMIN_EMAIL`/`ADMIN_PASSWORD` while no `ROLE_PLATFORM_ADMIN` exists (deploy: `deploy/README.md`) |

Inside a module package: `XxxController` + `XxxService` holding the request/response **records** as nested types
(e.g. `BookingService.CreateBookingRequest`, `BookingService.BookingDto` with a static `of(entity)`).
Tiny CRUD modules (crm, packages, settings) may use repositories directly from the controller.

## Recipe: new endpoint (test-first)
0. **Tests first (TDD, JUnit 5):** turn every rule of the spec function into a named test — see step 6 for what to cover —
   run them and see them fail, then do steps 1–5 until they pass. Pure domain logic gets plain JUnit unit tests
   (`@ParameterizedTest` for boundary tables).
1. Schema change? → entity in backend-common + new Flyway migration (see backend-common/CLAUDE.md).
2. Request record with Jakarta Validation matching the spec (`@NotBlank @Size(max=…)`, `@Pattern(regexp = StaffService.VN_PHONE)`, …).
   Response record whose field names equal the spec / `src/types/index.ts` in the frontend.
3. Service method `@Transactional` (`readOnly = true` for reads). Use the injected `Clock` for "today" (`LocalDate.now(clock)`),
   never `LocalDate.now()`. Business errors: `throw AppException.badRequest|conflict|forbidden(CODE, "Vietnamese message")`,
   missing rows: `new ResourceNotFoundException("khách hàng", id)`. Codes come from the spec's §1.3 table.
4. Controller returns `ApiResponse.ok(data[, "message"])`; `@ResponseStatus(HttpStatus.CREATED)` on creates;
   `@Valid @RequestBody`; caller via `@AuthenticationPrincipal Jwt jwt` → `CurrentUser.from(jwt)`.
5. Authorization (spec §1.5) in **two** places: a URL rule in `SecurityConfig.apiSecurity` for admin-only routes (so staff
   get 403 before body validation) **and** `@PreAuthorize(SecurityConfig.ADMIN)` on the method. Staff "own data" filtering uses
   `CurrentUser.staffMemberId()` in the service.
6. Integration test in `src/test/java/com/weddingevent/admin/` extending `IntegrationTest`: success, 400 field errors,
   403 for `staffToken()` on admin-only routes, each business error code. Use `uniqueFutureDate()` / `uniquePhone()` so tests
   don't collide (the DB is shared across tests); `json("{'a':1}")` swaps quotes.

## Many studios (ADR-0001) — read before touching data
- Studio-only entities extend `StudioOwnedEntity` (`@TenantId studioId`): Hibernate filters every query and fills
  `studio_id` on insert from `StudioTenantResolver` (`config/StudioTenantConfig`): explicit `StudioTenant.callAs/runAs`,
  else the JWT claim `studioId`, else `StudioTenant.NONE` (sees nothing). The studio is read when the session opens,
  so set it **outside** `@Transactional` (see `PublicContractController.inStudioOf`, `DemoDataSeeder.run`).
- Shared tables (`users`, `service_packages`, `notifications`, `studio_settings`) are filtered **by hand** with
  `CurrentStudio.require()` (e.g. `findByIdAndStudioId`). A new query on them must take the studio.
- Codes: `codes.next(Code.X, CurrentStudio.require())` (per-studio `studio_counters`).
- A new studio feature needs a case in `StudioIsolationApiTest` (studio B gets 404 / empty for A's rows).
- Tests calling tenant repositories directly wrap them in `inDemoStudio(() -> ...)`; the platform demo login is
  `platform@lumiere.vn / Platform@123`.

## Rules
- Concurrency-sensitive writes lock the row first (`AssetRepository.findByIdForUpdate`) or rely on `@Version` (Contract → 409 `CONCURRENT_MODIFICATION`).
- Escape user text in HTML (`HtmlUtils.htmlEscape`) — see `ContractDocumentRenderer`.
- Escape `%`/`_` in LIKE searches (see `SearchController`).
- Don't log phone numbers, tokens or passwords.
- Check: `./gradlew :admin-console:backend:build` (tests need Docker or `TEST_DATABASE_URL`). The build fails below
  **85% line coverage** (JaCoCo, measured over this module + backend-common; report:
  `build/reports/jacoco/test/html/index.html`). Never lower the gate or add exclusions to pass it — add tests.
