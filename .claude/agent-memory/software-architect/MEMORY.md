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
- `BrewService.processOrder()` validates ALL stock before ANY deduction, using PESSIMISTIC_WRITE lock via `IngredientRepository.findByIdWithLock()`
- XP leveling formula: `level = floor(sqrt(totalXp / 100)) + 1` — centralized in `Barista.levelForXp(long xp)` static method (model/Barista.java:25); both BaristaService and BrewService delegate to it
- `InsufficientStockException` maps to HTTP 409 Conflict (not 400) — confirmed in GlobalExceptionHandler
- `ErrorResponse` record includes `path` field (added per CLAUDE.md contract)
- `ArchitecturePlan.md` at project root is the living architecture review document — 34 items tracked across 5 review rounds (R1–R34)

## API Surface
| Controller | Base path | Notable endpoints |
|---|---|---|
| BaristaController | /api/baristas | CRUD + POST /{id}/practice |
| RecipeController | /api/recipes | CRUD |
| BrewController | /api/brew | POST /{recipeId} (legacy single), POST /order (multi-recipe) |
| StockController | /api/stock | GET all + PATCH /{id}/restock |
| FinancialController | /api/finance | GET /daily-report, GET /history |

## Known Issues / Areas for Improvement (as of 2026-03-13, Fifth Review)
READY FOR MERGE — no blocking issues remain.

PENDING (non-blocking):
- R7: Legacy endpoint `POST /api/brew/{recipeId}` — no sunset date documented
- R32: Read methods in BaristaService, RecipeService, FinancialService, StockService lack `@Transactional(readOnly = true)`
- R33: `BrewController.brew()` returns raw `Map<String, String>` instead of a typed DTO
- R34: `BrewIntegrationTest.setUp()` does not clean `daily_balances` table — risk of test pollution

RESOLVED (confirmed in code, Fifth Review additions):
- R29: StockController now injects StockService — service layer boundary respected
- R30: FinancialController now injects FinancialService — orElse logic moved to service
- R31: RecipeService.createRecipe() uses .toList() — no Collectors.toList() in codebase
- R6: XP formula centralised in Barista.levelForXp() static method

Previously resolved (R1–R28): see ArchitecturePlan.md for full history

## Coding Conventions (from CLAUDE.md)
- DTOs: always `record`, never Lombok @Data
- DI: constructor injection only, never @Autowired field injection
- Logging: SLF4J @Slf4j — never System.out.println
- Controllers: zero business logic
- Error JSON format: { timestamp, status, error, message, path }
