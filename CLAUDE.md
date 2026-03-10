# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
make db        # Start PostgreSQL container (required before running the app)
make run       # Run the Spring Boot app (http://localhost:8181)
make test      # Run all tests
make compile   # Compile only
make package   # Build JAR (skips tests)
make clean     # Remove compiled artifacts
make stop      # Stop Docker containers
make all       # Full pipeline: db → compile → test → package → run
```

Run a single test class:

```bash
mvn test -Dtest=ClassName
```

## Architecture

Spring Boot 3.2 REST API with Java 17 and PostgreSQL 16.

**Request flow:** Controller → Service → Repository (Spring Data JPA) → PostgreSQL

**Layers:**

- `controller/` — 5 REST controllers (`BaristaController`, `RecipeController`, `BrewController`, `StockController`, `FinancialController`)
- `service/` — Business logic (`BaristaService`, `RecipeService`, `BrewService`)
- `repository/` — JPA repositories (one per entity)
- `model/` — JPA entities (`Barista`, `Recipe`, `Ingredient`, `RecipeIngredient`, `DailyBalance`)
- `dto/` — Request/response objects
- `exception/` — `GlobalExceptionHandler` + typed exceptions → consistent `ErrorResponse` JSON

**Key design points:**

- `DataInitializer` (implements `CommandLineRunner`) seeds 7 ingredients and 7 recipes on startup if absent
- `BrewService` handles multi-recipe orders with atomic stock validation before deducting inventory and recording revenue in `DailyBalance`
- Barista XP/leveling formula: `level = floor(sqrt(totalXp / 100)) + 1`
- Recipes reference ingredients by ID in request bodies; `RecipeIngredient` is the junction entity
- Schema is auto-managed via `spring.jpa.hibernate.ddl-auto=update`

## Infrastructure

- **Database:** PostgreSQL 16 via Docker (`localhost:4040` → container port 5432)
- **App port:** 8181
- **Docker Compose:** defines `brewstack-db` container with volume `brewstack_data`
- **Credentials (dev only):** db=`coffeedb`, user=`brewstack`, pass=`brewstack123`

## Coding Conventions & Anti-Patterns

**Java 17 & Spring Boot Standards:**
- Use `record` for all classes inside the `dto/` package instead of traditional classes or Lombok `@Data`.
- **Dependency Injection:** Never use field injection (`@Autowired` on properties). Always use constructor injection, preferably via Lombok's `@RequiredArgsConstructor`.
- **Logging:** Never use `System.out.println()`. Always use SLF4J (`@Slf4j`) and log appropriately (`log.info`, `log.error`).

**Strict Architectural Boundaries:**
- **Controllers:** Must only handle HTTP routing, request parsing, and delegating to services. **ZERO** business logic or stock validation here.
- **Services:** All barista XP calculations and atomic stock validations must reside exclusively in the service layer (e.g., `BrewService`).

**Error Handling Format:**
- When creating or modifying custom exceptions, ensure they map to the `GlobalExceptionHandler` returning this exact JSON structure:
  ```json
  {
    "timestamp": "2026-03-10T10:00:00.000Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Insufficient ingredients for recipe",
    "path": "/api/v1/brews"
  }
  
## Language & Communication Policy
- **Conversation:** Always respond to my prompts, explain concepts in **Spanish**.
- **Code:** All generated code, including class names, variables, methods, database columns, and inline comments, must be strictly in **English**.


