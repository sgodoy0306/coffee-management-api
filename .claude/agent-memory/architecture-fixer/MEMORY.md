# Architecture Fixer — Persistent Memory

## Project: Coffee Management API
Spring Boot 3.2 / Java 17 / PostgreSQL 16

---

## Flyway Integration (R2)

- Spring Boot 3.2 ships with Flyway 10.x via BOM — no version needed in pom.xml.
- Flyway 10+ requires `flyway-database-postgresql` as a separate artifact alongside `flyway-core` for PostgreSQL support. Without it, Flyway cannot locate the PostgreSQL driver plugin and will fail at startup.
- Migration files live in `src/main/resources/db/migration/` (Flyway default location, no extra config needed).
- Naming convention: `V{version}__{Description}.sql` (double underscore separator).
- After adding Flyway, set `spring.jpa.hibernate.ddl-auto=validate` — Hibernate validates schema against entities but does NOT modify it.

## Credential Externalization (R3)

- Spring Boot `${VAR:default}` syntax in `application.properties` provides env-var override with dev fallback. Confirmed by Context7 Spring Boot docs.
- Docker Compose uses `${VAR:-default}` (dash before default) — different from Spring's colon-only syntax. Both syntaxes allow the container to run without a `.env` file in dev.
- `.env.example` acts as the contract for new developers — lists all required variables with dev-safe example values, never committed with real secrets.
- `.env` must be added to `.gitignore` before any `.env` file is created. Use `printf` not `echo` when appending to avoid missing-newline corruption in `.gitignore`.
- The two variable sets are intentionally distinct: `DB_URL/DB_USER/DB_PASSWORD` for Spring Boot, `POSTGRES_DB/POSTGRES_USER/POSTGRES_PASSWORD` for Docker Compose.

## Entity → SQL Type Mapping (Hibernate + PostgreSQL 16)

| Java type          | Hibernate column type  | PostgreSQL DDL type  |
|--------------------|------------------------|----------------------|
| Long (IDENTITY PK) | bigserial              | BIGSERIAL            |
| Long (non-PK)      | bigint                 | BIGINT               |
| Integer            | integer                | INTEGER              |
| Double             | float8                 | FLOAT8               |
| String             | varchar(255)           | VARCHAR(255)         |
| BigDecimal         | numeric(p,s)           | NUMERIC(p, s)        |
| LocalDate (PK)     | date                   | DATE                 |

## Column Naming (Hibernate default — snake_case)

- `totalXp` → `total_xp`
- `baseXpReward` → `base_xp_reward`
- `currentStock` → `current_stock`
- `minimumThreshold` → `minimum_threshold`
- `quantityRequired` → `quantity_required`
- `imageUrl` → `image_url`
- `totalRevenue` → `total_revenue`
- `totalOrders` → `total_orders`

## Table Names (explicit @Table annotations in this project)

- `Barista` → `baristas`
- `Ingredient` → `ingredients`
- `Recipe` → `recipes`
- `RecipeIngredient` → `recipe_ingredients`
- `DailyBalance` → `daily_balances`

## Recurring Anti-Patterns Observed

- Model classes use Lombok `@Data` (not records) — models are JPA entities so this is acceptable; records are required only in `dto/` package.
- No violations found in model layer beyond the architecture task itself.

## Links to Detail Files

- (none yet)
