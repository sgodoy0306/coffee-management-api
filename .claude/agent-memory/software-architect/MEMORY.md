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

## Known Issues / Areas for Improvement (as of 2026-03-14, Seventh Review)
NOT PRODUCTION-READY — R48 (missing pom.xml dependencies) is blocking. R42, R43, R44, R47, R53 also block production.

OPEN (Seventh Review — R42–R54):
- R42 CRITICAL: management.endpoint.health.show-details=always — exposes infra without auth → change to when-authorized
- R43 CRITICAL: ddl-auto=validate with Flyway = dual source of truth → change to none
- R44 HIGH: BrewService uses recipeRepository.findById() not findByIdWithIngredients() — LAZY load risk in order path
- R45 HIGH: Barista.name (and others) missing @Column(nullable=false) and NOT NULL in V1 migration
- R46 HIGH: Recipe.ingredients missing @BatchSize — no N+1 safety net for LAZY collection
- R47 HIGH: POST /api/brew/order returns 200 OK instead of 201 Created
- R48 HIGH BLOCKING: Flyway, Actuator, Testcontainers used in code/tests but NOT declared in pom.xml — integration tests cannot compile
- R49 MEDIUM: ingredientRepository.save() called per ingredient in loop — redundant with JPA dirty checking
- R50 MEDIUM: DailyBalanceNotFoundException declared + handler registered but never thrown — dead code
- R51 MEDIUM: V1__init.sql missing NOT NULL on name/unit columns — BD does not enforce domain integrity
- R52 MEDIUM: updateRecipe() cannot update ingredients — domain functionality gap (no RecipeIngredient update)
- R53 MEDIUM: ErrorResponse.timestamp (LocalDateTime) serializes as JSON array without jackson config — add write-dates-as-timestamps=false
- R54 LOW: findLowStockIngredients() declared in repo but unused — dead code + unused index

Previously resolved (R1–R41): see ArchitecturePlan.md for full history

## Coding Conventions (from CLAUDE.md)
- DTOs: always `record`, never Lombok @Data
- DI: constructor injection only, never @Autowired field injection
- Logging: SLF4J @Slf4j — never System.out.println
- Controllers: zero business logic
- Error JSON format: { timestamp, status, error, message, path }
