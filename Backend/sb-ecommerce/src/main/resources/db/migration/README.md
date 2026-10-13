# Database migrations

Flyway owns this schema. Migrations in this directory are applied at startup, in
version order, and recorded in the `flyway_schema_history` table.

`spring.jpa.hibernate.ddl-auto` is set to `validate`, so Hibernate checks the
entity mappings against the real schema on boot and fails fast if they have
drifted. Hibernate no longer creates or alters anything.

## Why this directory starts empty

Your existing database was built by Hibernate's `ddl-auto`, not by a migration,
so there is no authoritative SQL for it anywhere in the repository. Rather than
ship a hand-written baseline that has never been executed - and which would
block application startup if a single column type were wrong - generate it from
the source of truth.

`spring.flyway.baseline-on-migrate=true` means Flyway will adopt your current
database as-is, so the application runs fine with no migrations present.

## Generating the baseline (recommended)

Let Hibernate write out the exact DDL it expects, then keep that as V1.

1. Temporarily add these two lines to `application.properties`:

   ```
   spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create
   spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/schema.sql
   ```

2. Start the application once, with `DDL_AUTO=none` and `FLYWAY_ENABLED=false`:

   ```
   DDL_AUTO=none FLYWAY_ENABLED=false ./mvnw spring-boot:run
   ```

3. Stop it. `target/schema.sql` now contains Hibernate's expected schema.

4. Move it into place and remove the two temporary lines:

   ```
   cp target/schema.sql src/main/resources/db/migration/V1__baseline.sql
   ```

5. Review it. In particular check how `Product.productId` is generated - it is
   the only entity using `GenerationType.AUTO` while every other one uses
   `IDENTITY`, so Hibernate backs it with a sequence rather than an identity
   column. That inconsistency is worth resolving, but do it as a deliberate
   migration (V2) rather than by editing V1 after the fact.

6. Start normally. On your existing database Flyway baselines at version 0 and
   applies V1; because the tables already exist you will want V1 to be
   idempotent (`CREATE TABLE IF NOT EXISTS ...`), or mark it as already applied:

   ```
   ./mvnw flyway:baseline -Dflyway.baselineVersion=1
   ```

   On a fresh database V1 creates everything from scratch.

## Alternative: dump the live schema

If you would rather capture exactly what is in your database today, including
anything applied by hand:

```
pg_dump --schema-only --no-owner --no-privileges \
  -h localhost -U postgres sb-ecom \
  > src/main/resources/db/migration/V1__baseline.sql
```

Then strip the `CREATE DATABASE`/`\connect` preamble and any
`flyway_schema_history` table before committing.

`V1__baseline.sql.template` in this directory is a hand-written starting point
for reference. It has **not** been executed or validated against Hibernate -
treat it as a sketch, not a migration. Flyway ignores it because the filename
does not end in `.sql`.

## Adding a migration

Name it `V2__short_description.sql`, `V3__...` and so on. Never edit a migration
that has already run anywhere - Flyway checksums them and will refuse to start.
