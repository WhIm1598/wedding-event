# backend-common — shared Java domain library

Plain `java-library` (no main class) used by `admin-console/backend` and, later, `client-console/backend`.
Package root `com.weddingevent.common`. Lombok `@Getter/@Setter` on entities.

| Path | Contents |
|---|---|
| `domain/` | JPA entities extending `BaseEntity` (UUID id, `createdAt`, `updatedAt`) — `User`, `StaffMember`, `StaffTask`, `ServicePackage`, `Booking`, `Asset`, `AssetReservation`, `Lead`, `Contract`, `PaymentTransaction`, `Notification`, `StudioSettings` (single row, id 1) |
| `domain/enums/` | One enum per file; values exactly as the spec (§1.6 state machines). Stored as `VARCHAR` (`@Enumerated(EnumType.STRING)`) |
| `repository/` | Spring Data repositories, **one top-level interface per file** (nested interfaces are not scanned). Projection interfaces may be nested |
| `api/ApiResponse` | Envelope `{success, code, message, data, errors}`: `ApiResponse.ok(data[, message])`, `FieldError(field, message)` |
| `exception/` | `AppException.badRequest/conflict/unauthorized/forbidden(code, message)`, `ResourceNotFoundException`, `GlobalExceptionHandler` (validation → 400 `BAD_REQUEST` + field errors, optimistic lock → 409 `CONCURRENT_MODIFICATION`) |
| `support/CodeGenerator` | Display codes from PostgreSQL sequences: `NV-001`, `HD-102`, `TRX-001` |
| `util/VndFormatter` | `vnd(BigDecimal)` → `245.000.000đ`, `date(LocalDate)` → `dd/MM/yyyy` |
| `src/main/resources/db/migration/` | **Flyway migrations — the only way the schema changes.** Rules: `src/main/resources/db/README.md` |

## Recipe: new table / column
1. Add `V{n}__lower_snake_case.sql` with `n` = highest existing version + 1 (`MigrationConventionsTest` enforces naming and no gaps).
   Never edit, rename or delete an existing migration. Add columns nullable or with a default; tighten later.
2. Update/add the entity: column names via `@Column(name = "snake_case")`, money `BigDecimal` ↔ `NUMERIC(15,0)`,
   dates `LocalDate`, timestamps `Instant` ↔ `TIMESTAMPTZ`, enums as strings. Hibernate runs `ddl-auto=validate`,
   so any mismatch fails application startup and the integration tests.
3. Business state transitions belong on the entity (e.g. `Contract.applyPayment`), not in controllers.
4. Repository queries: derived names or JPQL `@Query` with named `@Param`s; filter "active" rows explicitly (e.g. not `CANCELLED`).

## Rules
- No Spring Boot auto-configuration or web controllers here — only domain + shared infrastructure.
- No demo data in migrations (the admin backend's `DemoDataSeeder` does that); reference rows the app needs are fine.
- Check: `./gradlew :backend-common:build` (also run the admin backend build — it exercises every migration).
