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

## State as of 2026-03-15 (Eighth Review — dev branch)
All R42–R54 from Seventh Review now RESOLVED in dev branch.
R42: health.show-details=when-authorized. R43: ddl-auto=none. R44: findByIdWithIngredients used.
R45+R51: V4__add_not_null_constraints.sql created. R46: @BatchSize(20) on Recipe.ingredients.
R47: POST /api/brew/order returns 201. R48: pom.xml has Flyway, Actuator, Testcontainers declared.
R49: save() loop removed. R50: DailyBalanceNotFoundException removed. R52: updateRecipe handles ingredients.
R53: write-dates-as-timestamps=false in application.properties. R54: GET /api/stock/low implemented.

OPEN (Eighth Review — R55–R62, tests only):
- R55 HIGH: RecipeServiceTest missing createRecipe/updateRecipe unit tests
- R56 HIGH: StockServiceTest missing (file exists but may be incomplete)
- R57 HIGH: No integration test validates ErrorResponse body shape
- R58 MEDIUM: BaristaServiceTest missing CRUD methods (findById, create, update)
- R59 MEDIUM: FinancialServiceTest missing (file exists but may be incomplete)
- R60 MEDIUM: StockIntegrationTest missing
- R61 MEDIUM: 14 endpoints without HTTP integration tests
- R62 LOW: rating=0 test case in BaristaServiceTest covers invalid scenario

BLOCKING FOR MERGE (2026-03-15): BrewServiceTest has 7 failing tests.
Root cause: stubs use recipeRepository.findById() but BrewService calls findByIdWithIngredients().
Tests processOrder_happyPath_*, processOrder_insufficientStock_*, processOrder_sharedIngredient_* all fail.
Also: processOrder_unknownRecipe test has UnnecessaryStubbingException (stub for findById(999L) is never called).
Test score: 85 total, 3 failures, 4 errors, 0 skipped.

Previously resolved (R1–R54): see ArchitecturePlan.md for full history

## Coding Conventions (from CLAUDE.md)
- DTOs: always `record`, never Lombok @Data
- DI: constructor injection only, never @Autowired field injection
- Logging: SLF4J @Slf4j — never System.out.println
- Controllers: zero business logic
- Error JSON format: { timestamp, status, error, message, path }
