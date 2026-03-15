# Software Architect Memory — Coffee Management API

## Project Overview
- **Stack:** Spring Boot 3.2 / Java 17 / PostgreSQL 16 / Maven / Lombok
- **App port:** 8181 | **DB port (host):** 4040 → container 5432
- **Package root:** `com.brewstack.api`

## Domain Model (5 entities)
- `Barista` — id, name, level, totalXp (table: baristas)
- `Recipe` — id, name, baseXpReward, price (NUMERIC 10,2), imageUrl (table: recipes)
- `Ingredient` — id, name, currentStock (BigDecimal NUMERIC 10,3), minimumThreshold (BigDecimal NUMERIC 10,3), unit (table: ingredients)
- `RecipeIngredient` — join entity Recipe ↔ Ingredient with quantityRequired (BigDecimal NUMERIC 10,3) (table: recipe_ingredients)
- `DailyBalance` — PK is LocalDate, totalRevenue (NUMERIC 10,2), totalOrders (table: daily_balances)

## Key Architectural Decisions
- Classic 3-layer: Controller → Service → Repository (Spring Data JPA)
- DTOs are Java `record`s (enforced by CLAUDE.md); model entities use Lombok @Data
- `GlobalExceptionHandler` (@RestControllerAdvice) centralises all error mapping to `ErrorResponse` record
- `DataInitializer` (CommandLineRunner) seeds 7 ingredients + 7 recipes idempotently on startup
- `BrewService.processOrder()` validates ALL stock before ANY deduction, using PESSIMISTIC_WRITE lock via `IngredientRepository.findByIdWithLock()`
- XP leveling formula: `level = floor(sqrt(totalXp / 100)) + 1` — centralized in `Barista.levelForXp(long xp)` static method (model/Barista.java:25); both BaristaService and BrewService delegate to it
- `InsufficientStockException` maps to HTTP 409 Conflict (not 400) — confirmed in GlobalExceptionHandler
- `ErrorResponse` record includes `path` field (added per CLAUDE.md contract)
- `ArchitecturePlan.md` at project root is the living architecture review document — 41 items tracked across 6 review rounds (R1–R41)
- `BrewService.processOrder()` accumulates a `totalDemanded` Map before stock validation to prevent shared-ingredient stock going negative (R35 fix)
- `Ingredient`, `Recipe`, `DailyBalance` all have `@Index` annotations for non-PK query columns; Flyway migration `V3__add_indexes.sql` mirrors them
- `FinancialService.getHistory()` is paginated — `DailyBalanceRepository.findAllByOrderByDateDesc(Pageable)`, controller uses `@PageableDefault(size=30)`
- Legacy endpoint `POST /api/brew/{recipeId}` was removed (PR #42)

## API Surface
| Controller | Base path | Notable endpoints |
|---|---|---|
| BaristaController | /api/baristas | CRUD + POST /{id}/practice |
| RecipeController | /api/recipes | CRUD |
| BrewController | /api/brew | POST /order (multi-recipe only — legacy endpoint removed) |
| StockController | /api/stock | GET all + PATCH /{id}/restock |
| FinancialController | /api/finance | GET /daily-report, GET /history (paginated) |

## State as of 2026-03-15 (Production Readiness Review — dev branch)
All R1–R62 items RESOLVED. Test suite: 85 tests, 0 failures, 0 errors, BUILD SUCCESS.
Test breakdown: BrewServiceTest(8), BaristaServiceTest(19), RecipeServiceTest(7), StockServiceTest(7),
FinancialServiceTest(8), BrewIntegrationTest(5), BaristaIntegrationTest(14), RecipeIntegrationTest(7),
StockIntegrationTest(7), FinancialIntegrationTest(3).

OPEN (Production Readiness Analysis — critical gaps identified 2026-03-15):
CRITICAL:
- No authentication/authorization (Spring Security absent — all endpoints are public)
- Credentials hardcoded in application.properties (DB_USER default = brewstack, DB_PASSWORD default = brewstack123)
- No application-prod.properties profile — same config for dev and prod
- No Dockerfile or container image — no deployment artifact
- No CI/CD pipeline (.github/ directory absent)
- No rate limiting — BrewService vulnerable to inventory exhaustion attacks

HIGH:
- No Springdoc/OpenAPI dependency — no API documentation
- README documents removed endpoint POST /api/brew/{recipeId} as still existing (stale doc)
- README error table says 400 for insufficient stock (actual is 409)
- No connection pool tuning (HikariCP defaults — no explicit max-pool-size)
- DataInitializer runs on every startup in prod (no profile guard)
- No @Transactional on DataInitializer.run() for the full batch (each seed call is its own implicit transaction)
- StockController.restock() has no @Transactional lock — concurrent restocks safe only at DB level
- No Spring Boot version pinning beyond BOM (currently 3.2.0 — not latest 3.2.x patch)

MEDIUM:
- No Dockerfile; deployment relies on developer running `mvn spring-boot:run`
- No structured logging format (plain text, not JSON) — hard to ingest into log aggregators
- Actuator metrics endpoint exposed without auth (only health+info+metrics, no security)
- No distributed tracing (no Micrometer Tracing / OpenTelemetry)
- @Index on DailyBalance.date is redundant (date is already the PK)
- RecipeController uses plain findAll (no pagination on recipe list)

LOW:
- Spring Boot 3.2.0 → latest patch is 3.2.x (minor CVE exposure)
- No @UniqueConstraint on barista name or ingredient name (only existsByName guards at service level)
- No max page size guard on Pageable — client can request page size = Integer.MAX_VALUE
- README out of date (missing GET /api/stock/low endpoint)

Previously resolved (R1–R62): see ArchitecturePlan.md for full history

## Coding Conventions (from CLAUDE.md)
- DTOs: always `record`, never Lombok @Data
- DI: constructor injection only, never @Autowired field injection
- Logging: SLF4J @Slf4j — never System.out.println
- Controllers: zero business logic
- Error JSON format: { timestamp, status, error, message, path }
