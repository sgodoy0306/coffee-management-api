# Software Architect Memory — Coffee Management API

## Project Overview
- **Stack:** Spring Boot 3.2 / Java 17 / PostgreSQL 16 / Maven / Lombok
- **App port:** 8181 | **DB port (host):** 4040 → container 5432
- **Package root:** `com.brewstack.api`

## Domain Model (5 entities)
- `Barista` — id, name, level, totalXp (table: baristas)
- `Recipe` — id, name, baseXpReward, price (NUMERIC 10,2), imageUrl (table: recipes)
- `Ingredient` — id, name, currentStock (Double), minimumThreshold (Double), unit (table: ingredients)
- `RecipeIngredient` — join entity Recipe ↔ Ingredient with quantityRequired (Double) (table: recipe_ingredients)
- `DailyBalance` — PK is LocalDate, totalRevenue (NUMERIC 10,2), totalOrders (table: daily_balances)

## Key Architectural Decisions
- Classic 3-layer: Controller → Service → Repository (Spring Data JPA)
- DTOs are Java `record`s (enforced by CLAUDE.md); model entities use Lombok @Data
- `GlobalExceptionHandler` (@RestControllerAdvice) centralises all error mapping to `ErrorResponse` record
- `DataInitializer` (CommandLineRunner) seeds 7 ingredients + 7 recipes idempotently on startup
- `BrewService.processOrder()` validates ALL stock before ANY deduction (all-or-nothing semantic without DB-level lock)
- XP leveling formula: `level = floor(sqrt(totalXp / 100)) + 1` — duplicated in BaristaService.addExperience() and BrewService.processOrder()

## API Surface
| Controller | Base path | Notable endpoints |
|---|---|---|
| BaristaController | /api/baristas | CRUD + POST /{id}/practice |
| RecipeController | /api/recipes | CRUD |
| BrewController | /api/brew | POST /{recipeId} (legacy single), POST /order (multi-recipe) |
| StockController | /api/stock | GET all + PATCH /{id}/restock |
| FinancialController | /api/finance | GET /daily-report, GET /history |

## Known Issues / Areas for Improvement
- `StockController` injects `IngredientRepository` directly — violates layering (no service)
- `FinancialController` injects `DailyBalanceRepository` directly — same issue
- XP formula duplicated in two services (BaristaService + BrewService) — DRY violation
- Stock deduction saves each ingredient one-by-one in a loop inside a @Transactional — N individual saves instead of batch
- `spring.jpa.hibernate.ddl-auto=update` is unsafe for production
- `spring.jpa.show-sql=true` leaks SQL in production logs
- No API versioning in URL prefix (path is /api/... not /api/v1/...)
- No security layer (no Spring Security, no auth)
- No observability tooling (no Actuator, no Micrometer, no distributed tracing)
- Credentials hardcoded in application.properties (dev-only acceptable but no env-var mechanism)
- `processBrew(Long recipeId)` (single-recipe brew) is a legacy endpoint that duplicates logic from processOrder — candidate for removal

## Coding Conventions (from CLAUDE.md)
- DTOs: always `record`, never Lombok @Data
- DI: constructor injection only, never @Autowired field injection
- Logging: SLF4J @Slf4j — never System.out.println
- Controllers: zero business logic
- Error JSON format: { timestamp, status, error, message, path }
