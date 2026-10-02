# Database migrations (Flyway)

Flyway is the **only** thing that creates or changes tables. Hibernate runs with `ddl-auto: validate`: it never
touches the schema, it only checks at startup that the JPA entities in `backend-common` match it.

Migrations live here, in `backend-common/src/main/resources/db/migration/`, next to the entities they describe.
Every backend that depends on `backend-common` (admin-console now, client-console later) ships the same set and runs
`migrate` on startup; Flyway's lock on `flyway_schema_history` makes concurrent starts safe.

## Adding a change

1. Create `db/migration/V{n}__{what_changed}.sql`, where `n` is the highest existing version + 1
   (e.g. `V2__contract_installments.sql`). Lower-case snake_case description, no gaps, no duplicates —
   `MigrationConventionsTest` fails the build otherwise.
2. Update the JPA entity / enum in `backend-common` in the **same** PR. The integration tests start the app on an
   empty PostgreSQL, apply every migration and let Hibernate validate the result.
3. Run `./gradlew build`.

## Rules

- **Released migrations are immutable.** Never edit, rename or delete a `V*.sql` that is on `main`: Flyway stores a
  checksum and every existing database would refuse to start. Fix mistakes with a new migration. CI rejects PRs
  that modify or delete an existing migration, and the Claude agent may only add new ones.
- **Stay backward compatible** — two services and older deployments share the database:
  - new column: add it nullable (or with a default) → backfill → `SET NOT NULL` in a later migration;
  - rename: add the new column, copy data, switch the code, drop the old column in a later release;
  - never drop a column/table that the previous release still reads.
- One logical change per file; PostgreSQL 16 SQL is fine (no need for vendor-neutral SQL).
- Enums are stored as `VARCHAR` — adding a Java enum value needs no migration, but widen the column if the new name is longer.
- **No demo data in migrations.** Sample data comes from `DemoDataSeeder` (profile `dev`). Migrations may insert
  *reference* data the app needs to run (e.g. the single `studio_settings` row).
- Repeatable migrations (`R__name.sql`, re-run when their checksum changes) are allowed for views/functions only.

## Useful commands

```bash
# What has been applied to my local database?
docker exec -it wedding_postgres_dev psql -U wedding_admin -d wedding_db \
  -c "SELECT installed_rank, version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank"

# Start over locally (drops the volume, the app re-applies every migration on next start)
docker compose -f docker-compose.dev.yml down -v && docker compose -f docker-compose.dev.yml up -d
```

`flyway clean` is disabled (`spring.flyway.clean-disabled: true`) so a misconfigured environment can never wipe a database.
