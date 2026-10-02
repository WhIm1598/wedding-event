# admin-console/backend — Lumière Studios admin API

Spring Boot 4.1 · Java 21 · Spring Security 7 (stateless JWT HS256) · Spring Data JPA / Hibernate 7 · Flyway · PostgreSQL 16.
Port 8082. Every route is under `/api/v1/admin/**` (nginx forwards only that prefix here, plus `/api/v1/public/**`).
Entities, enums, repositories and migrations live in **backend-common**, not here.

## Layout (`src/main/java/com/weddingevent/admin/`)
One package per spec module: `auth`, `dashboard`, `booking`, `packages`, `asset`, `staff`, `crm`, `contract`, `finance`,
`notification`, `search`, `settings`. Shared code:
| Package | Contents |
|---|---|
| `security/` | `SecurityConfig` (URL rules, JWT encoder/decoder), `TokenService`, `CurrentUser.from(jwt)` (`id`, `role`, `staffMemberId`, `isAdmin()`), `SecurityErrorHandler` |
| `config/` | `AppProperties` (`app.*`: zone, jwt, cors, public-base-url, demo-data), `TimeConfig` (`Clock` in Asia/Ho_Chi_Minh) |
| `support/` | `DateRange` (day / Monday-based week / month, instant bounds) |
| `notification/` | `NotificationService.notify(title, description)` — call it for events the team should see |
| `demo/` | `DemoDataSeeder` — seeds the empty dev/test database (profile flag `app.demo-data.enabled`) |

Inside a module package: `XxxController` + `XxxService` holding the request/response **records** as nested types
(e.g. `BookingService.CreateBookingRequest`, `BookingService.BookingDto` with a static `of(entity)`).
Tiny CRUD modules (crm, packages, settings) may use repositories directly from the controller.

## Recipe: new endpoint
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

## Rules
- Concurrency-sensitive writes lock the row first (`AssetRepository.findByIdForUpdate`) or rely on `@Version` (Contract → 409 `CONCURRENT_MODIFICATION`).
- Escape user text in HTML (`HtmlUtils.htmlEscape`) — see `ContractDocumentRenderer`.
- Escape `%`/`_` in LIKE searches (see `SearchController`).
- Don't log phone numbers, tokens or passwords.
- Check: `./gradlew :admin-console:backend:build` (tests need Docker or `TEST_DATABASE_URL`).
