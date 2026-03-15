# Coffee Management API - Architect Memory

## Project Identity
- Name: Coffee Management API (brewstack)
- Stack: Spring Boot 3.2, Java 17, PostgreSQL 16, Lombok, Spring Data JPA
- Port: 8181 (app), 4040 (Postgres via Docker)
- Package root: `com.brewstack.api`
- Build tool: Maven; dev convenience via Makefile

## Architecture Layer Map
- Controller → Service → Repository (Spring Data JPA) → PostgreSQL
- 5 controllers: Barista, Recipe, Brew, Stock, Financial
- 3 services: BaristaService, RecipeService, BrewService
- 4 repositories: Barista, Recipe, Ingredient, DailyBalance
- 5 models: Barista, Recipe, Ingredient, RecipeIngredient, DailyBalance

## Key Architectural Facts
- BrewService.processOrder() is the most critical path: validates stock atomically across multi-recipe orders, deducts stock, updates DailyBalance, awards XP to Barista — all in one @Transactional method
- XP level formula (duplicated in two places): `(int) Math.floor(Math.sqrt(totalXp / 100.0)) + 1`
- `processBrew(recipeId)` is a legacy single-recipe endpoint that does NOT grant XP — inconsistency with processOrder
- StockController bypasses the service layer: calls IngredientRepository directly
- FinancialController bypasses the service layer: calls DailyBalanceRepository directly
- DataInitializer uses CommandLineRunner (not @Transactional, not Flyway/Liquibase)
- ddl-auto=update in application.properties (dangerous for production)
- Credentials hardcoded in application.properties and docker-compose.yml (no env var substitution)
- No Spring Security, no authentication/authorization
- No Actuator/Micrometer dependency
- No caching layer
- Only one test exists: ApiApplicationTests (context load only)
- Ingredient.currentStock and quantityRequired use Double (floating-point), not BigDecimal
- `@Transactional` missing on `createBarista` and `deleteBarista` in BaristaService
- Race condition in processOrder() FIXED (2026-03-10, R1): both loops now call `ingredientRepository.findByIdWithLock(id)` which issues `SELECT ... FOR UPDATE` via `@Lock(LockModeType.PESSIMISTIC_WRITE)` — import is `jakarta.persistence.LockModeType`
- Race condition in processBrew() is NOT yet fixed — same unguarded pattern, candidate for a follow-up task
- `findByIdWithLock(Long id)` is the established locked-fetch method name in IngredientRepository

## Decisions Recorded
- See full architectural review produced 2026-03-10 for prioritized recommendations
