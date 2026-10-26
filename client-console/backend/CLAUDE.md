# client-console/backend — WedPlanner mobile API

Spring Boot 4.1 · Java 21 · Spring Security 7 (stateless JWT HS256) · Spring Data JPA / Hibernate 7 · Flyway · PostgreSQL 16 ·
STOMP over WebSocket. Port 8081. Routes `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/client/**`, `/api/v1/messages/**`
and WebSocket `/ws` (nginx routing: spec §1.4). Endpoints are specified in **Part A** of
`docs/features/detailed_functional_specification.md`; the Flutter repositories in `client-console/mobile/lib/data/repositories`
call them, so paths and JSON names must stay identical. Entities, repositories and migrations live in **backend-common**.

## Layout (`src/main/java/com/weddingevent/client/`)
| Package | Contents |
|---|---|
| `auth/` | `AuthController` (`/auth/login`, `/google`, `/send-otp`, `/verify-otp`, `/me`), `AuthService` (FN-AUTH-01, Google), `OtpService` (FN-AUTH-02), adapters `OtpSender`/`LoggingOtpSender`, `GoogleIdTokenVerifier`/`NimbusGoogleIdTokenVerifier`, `AuthConfig` (their beans) |
| `user/` | `UserController` (`/users/role`, `/users/me`), `UserService` (FN-AUTH-03/04, `VN_PHONE`), `UserDto` (the spec's `user` object) |
| `plan/` | `PlanController`/`PlanService` (FN-PLAN-01..05), `PlanDtos` (response records + the % formulas), `DefaultChecklist`, `WeddingStyle` |
| `plan/ai/` | `AiPlannerClient` (FN-PLAN-03 contract), `HttpAiPlannerClient` (ai-planner-service), `RuleBasedAiPlanner` (fallback when `app.ai-planner.base-url` is empty), `AiPlannerConfig` |
| `guest/` | `GuestController`/`GuestService` (FN-GUEST-01/02) |
| `catalog/` | `CatalogController`/`CatalogService` (FN-SVC-01/02: active packages of vendors and of the studio) |
| `notification/` | `ClientNotificationController`/`ClientNotificationService` (FN-NOTI-01 inbox; `notify(userId, kind, title, body)` stores + pushes after commit), `PushSender`/`LoggingPushSender` |
| `message/` | `MessageController` (REST), `ChatStompController` (`SEND /app/chat`), `MessageService` (FN-MSG-01), `WebSocketConfig`, `StompAuthInterceptor` (CONNECT/SUBSCRIBE/SEND rules), `PresenceTracker` |
| `security/` | `SecurityConfig` (URL rules, JWT encoder/decoder), `TokenService`, `UserAuthenticationConverter` (role **from the DB** per request), `CurrentUser.id(jwt)`, `SecurityErrorHandler` |
| `config/` | `AppProperties` (`app.*`: zone, jwt, cors, otp, google, ai-planner, demo-data), `TimeConfig` (`Clock` in Asia/Ho_Chi_Minh) |
| `demo/` | `ClientDemoDataSeeder` — demo couple/vendors/catalog when `app.demo-data.enabled` (dev, test) |

Module package = `XxxController` + `XxxService` with request/response **records** nested in the service (or `PlanDtos`).

## Rules specific to this service
- **Roles are not in the token.** A mobile user picks the role after signing in, and a locked account must stop working
  at once, so `UserAuthenticationConverter` loads the user on each request. Studio accounts (`ROLE_ADMIN/STAFF`) are
  rejected here (and mobile accounts by the admin backend). Tokens use issuer `wedding-event-client`; admin tokens fail.
- Authorization in **two** places: URL rule in `SecurityConfig.apiSecurity` (plans/guests → `ROLE_COUPLE`; services and
  messages → couple or vendor; `/auth/me`, `/users/**`, notifications → any signed-in user) **and** `@PreAuthorize`
  (`SecurityConfig.COUPLE`, `COUPLE_OR_VENDOR`) on the controller.
- "My plan" = the `PLANNING` plan the user owns **or** joined as partner (`WeddingPlanRepository.findActiveFor`). A `DRAFT`
  (created at role selection) is not a plan: `hasPlan=false`, `my-plan` → `data: null`. Anything outside the caller's
  plan is `404 NOT_FOUND`, never 403.
- Validation that depends on today (wedding date ≥ today + 7) or on several fields (allocation ≤ 150%) throws
  `AppException.invalidField(field, message)` → same 400 shape as Jakarta Validation.
- Notifications: mobile rows have `recipient_user_id`; always query by recipient. Use `ClientNotificationService.notify`
  so the inbox and push stay in sync; push/WebSocket sends happen **after commit** (`ClientNotificationService.afterCommit`).
- `users.email` only ever holds a **proven** address: a profile change waits in `pending_email` until its OTP is confirmed,
  each OTP proves only the `address` it was sent to (max 5 codes/hour per account), and Google login accepts only
  Google-verified addresses and never links to an unverified account (`OtpService`, `AuthService.loginWithGoogle`).
  Google users are matched by `users.google_sub` (the ID token's `sub`) first, e-mail only to link an account once;
  an address already tied to another `sub` is refused (recycled Gmail address).
- Slow external calls (AI planner) run **outside** the DB transaction; save the result with `TransactionTemplate`.
- External adapters (`OtpSender`, `PushSender`, `GoogleIdTokenVerifier`, `AiPlannerClient`) are beans: tests replace them in
  `src/test/.../TestDoubles.java`. Never log OTP codes or tokens outside `app.otp.log-code=true` (dev).
- STOMP security lives in `StompAuthInterceptor`; a new destination needs a rule there.
- Single-instance assumptions to revisit when scaling out: in-memory STOMP broker and `PresenceTracker`.

## Recipe: new endpoint (test-first)
Same as [admin-console/backend/CLAUDE.md](../../admin-console/backend/CLAUDE.md) "new endpoint", with these differences:
tests extend `ClientIntegrationTest` (real PostgreSQL, random port), create their own users with
`createUser(role, verified)` + `token(user)` (or `coupleToken()`, `createPlan(token)`), and use unique names because the
database is shared by all tests. Cover success, 400 field errors, 403 for the wrong role, 404 for another couple's data
and every business code.

## Error codes used here (spec §1.3, client table)
`INVALID_CREDENTIALS` 401, `ACCOUNT_LOCKED` 403, `INVALID_GOOGLE_TOKEN` 401, `INVALID_OTP` 400, `OTP_EXPIRED` 400,
`ROLE_ALREADY_SET` 409, `EMAIL_EXISTS` 409, `PLAN_ALREADY_EXISTS` 409, `INVALID_SYNC_CODE` 400, `CANNOT_JOIN_OWN_PLAN` 409,
`PLAN_ALREADY_SYNCED` 409, `INVALID_RECIPIENT` 400, `AI_SERVICE_UNAVAILABLE` 503, plus the generic ones.

## Not built (no spec yet)
Vendor workspace endpoints (dashboard, projects, conversations list, service management — the app's `VendorRepository`
works in mock mode only), refresh-token endpoint, conversation history, e-mail delivery, FCM, Redis-backed presence.

## Check
`./gradlew :client-console:backend:build` — tests need Docker (Testcontainers) or an empty PostgreSQL via
`TEST_DATABASE_URL` (+ `_USERNAME`/`_PASSWORD`). Fails below **85% line coverage** of this module's classes
(report: `build/reports/jacoco/test/html/index.html`); backend-common is measured by the admin backend's gate.
