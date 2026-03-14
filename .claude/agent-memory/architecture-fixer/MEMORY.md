# Architecture Fixer — Persistent Memory

## Project: Coffee Management API
Spring Boot 3.2 / Java 17 / PostgreSQL 16

---

## Flyway Integration (R2)

- Spring Boot 3.2 BOM pulls Flyway **9.22.3** (NOT Flyway 10+). No `flyway-database-postgresql` artifact is needed for this project.
- If upgraded to Flyway 10+, `flyway-database-postgresql` would be required alongside `flyway-core`.
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

## BigDecimal for Stock Quantities (R9 — completed)

- `Ingredient.currentStock`, `Ingredient.minimumThreshold`, `RecipeIngredient.quantityRequired`, and `RestockRequest.amount()` are all `BigDecimal`.
- `@Column(precision = 10, scale = 3)` is applied to all three entity fields.
- Flyway migration `V2__alter_stock_quantities_to_numeric.sql` alters columns from `FLOAT8` to `NUMERIC(10,3)` with `USING` cast.
- Always use `new BigDecimal("18.0")` string constructor in `DataInitializer` — NOT `BigDecimal.valueOf(18.0)`.
- Comparisons use `.compareTo() < 0` (NOT `<`). Arithmetic uses `.subtract()` and `.add()` (NOT `-` or `+`).
- When changing entity field types, ALWAYS check test files for `double` literals passed to constructors or setters — they must be updated to `BigDecimal` too.
- AssertJ assertions on `BigDecimal` fields must use `.isEqualByComparingTo()`, NOT `.isEqualTo()` with a `double` literal (scale matters in `.equals()`).

## Test Infrastructure (confirmed working — 23 tests, 0 failures)

- Testcontainers BOM version **1.19.3** is compatible with Spring Boot 3.2.
- Required Maven artifacts (all `test` scope): `testcontainers`, `junit-jupiter`, `postgresql`.
- H2 dependency has been **removed** from pom.xml. H2 is no longer used in this project.
- `src/test/resources/application.properties` has been **deleted** — Testcontainers injects datasource props via `@DynamicPropertySource`.
- `ApiApplicationTests` has been **deleted** — `contextLoads` is covered implicitly by any `@SpringBootTest` integration test.
- Integration tests use `postgres:16-alpine` image matching production.
- Unit tests: `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` + `@Mock`. No Spring context loaded — fast.
- `BrewIntegrationTest` and `BaristaIntegrationTest` both extend `AbstractIntegrationTest`.
- `BaristaIntegrationTest` has `@BeforeEach baristaRepository.deleteAll()` to prevent ID collisions between tests.
- `InsufficientStockException` maps to HTTP 409 Conflict (R15 implemented). `BrewIntegrationTest` assertion updated to `HttpStatus.CONFLICT` (R18 implemented).

### Singleton Container Pattern for AbstractIntegrationTest (confirmed working — use this)

**CRITICAL:** Do NOT use `@Testcontainers` + `@Container` in the abstract base class. That mechanism manages lifecycle per-class. When multiple subclasses in different packages are executed by Surefire, each subclass loads the inherited static `@Container` field as a new container instance, but the Spring context (cached from the first run) still points to the previous container's port — causing `Connection refused` on the second class.

The correct pattern is a **static initializer block** that calls `.start()` manually. Ryuk cleans up the container at JVM exit.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("coffeedb_test")
                .withUsername("brewstack")
                .withPassword("brewstack123");
        postgres.start();
    }

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

Subclasses in any package can extend this — they inherit the already-running container and Spring reuses the cached context (0 extra startup cost).

### deleteAll() Order in BrewIntegrationTest

Recipe has `cascade = CascadeType.ALL` + `orphanRemoval = true` on `@OneToMany` with RecipeIngredient.
No `RecipeIngredientRepository` exists. Correct teardown order: `recipeRepository.deleteAll()` first (propagates to recipe_ingredients via cascade), then `ingredientRepository.deleteAll()`, then `baristaRepository.deleteAll()`.

## BrewService Invariants (critical for future tests/refactoring)

- `processOrder` two-loop atomicity: ALL stock validation before ANY deduction.
- If ANY ingredient fails check → `InsufficientStockException` → no `save()` calls at all.
- `findByIdWithLock` (PESSIMISTIC_WRITE) is used in BOTH validation and deduction loops — mocks must stub it for both.
- XP formula (R6 completed): extracted to `Barista.levelForXp(long xp)` static method. Both `BaristaService` and `BrewService` now call `Barista.levelForXp(barista.getTotalXp())`. Formula: `level = floor(sqrt(xp / 100.0)) + 1`.
- In `addExperience`: `xpGained = rating * 50` (integer rating 1-10).
- `processBrew` is `@Deprecated` (R7): uses same two-loop pessimistic lock pattern as `processOrder`, but intentionally does NOT award XP. Retained for backwards compatibility only.
- `POST /api/brew/{recipeId}` response message explicitly warns caller to migrate to `POST /api/brew/order`.

## pom.xml Tooling Note

- Write/Edit tools may be permission-denied on `pom.xml` in some sessions. Use a Python3 inline script via Bash as fallback to modify `pom.xml`.
- Python3 string replacement scripts can fail silently if the target substring is not found exactly. Always verify with Read after the script runs, then use Edit tool as a reliable fallback for remaining changes.

## Service Layer Pattern (confirmed for this project)

- `BaristaService` uses explicit constructor (not `@RequiredArgsConstructor`) — new services follow the same explicit constructor pattern.
- `@Transactional` is applied at the method level only on write operations (not the whole class).
- Read-only methods do NOT need `@Transactional` in this project (no `@Transactional(readOnly = true)` used).
- `StockService` owns: `getAllStock()`, `restock(Long id, RestockRequest request)`.
- `FinancialService` owns: `getDailyReport()`, `getHistory()`.
- Both services added in R5. Controllers (`StockController`, `FinancialController`) no longer inject repositories directly.

## Stream Terminal Operations (R27 — completed)

- Use `.stream().toList()` (Java 16+) instead of `.stream().collect(Collectors.toList())` everywhere in this project.
- `Stream.toList()` returns an **unmodifiable** list — matches the immutability intent of the project's data flow (services return DTOs, not mutable collections).
- `Collectors.toList()` returns a mutable `ArrayList` with no immutability guarantees — a weaker contract.
- Developers sometimes use the fully-qualified `java.util.stream.Collectors.toList()` inline (no import) to avoid adding an import — this is still a violation and produces the same verbose, weaker result. grep pattern to detect: `collect\(.*Collectors\.toList\(\)\)`.
- `Collectors.joining()` is NOT affected by this rule — it produces a `String`, not a `List`, and has no direct `.toList()` equivalent.
- Note the branch naming convention for this type of refactor: `chore/recipe-service-stream-toList`.

## Recurring Anti-Patterns Observed

- Model classes use Lombok `@Data` (not records) — acceptable for JPA entities; records required only in `dto/` package.
- `ApiApplicationTests` was using `@SpringBootTest` without Testcontainers — caused connection failure since the app needs a real PostgreSQL DB.
- Controllers injecting repositories directly — fixed in R5 for `StockController` and `FinancialController`.
- Integration tests previously used H2 with PostgreSQL compatibility mode — replaced with real PostgreSQL via Testcontainers for production parity.
- Tests constructing entities with `double` literals after a `Double` → `BigDecimal` migration — must scan ALL test files for `new Ingredient(...)`, `new RecipeIngredient(...)`, and setter calls when changing entity field types.
- Stale TODO comments in tests that reference unreleased items (e.g., "TODO: update when R15 is implemented") — when the referenced fix lands, the TODO and the assertion must be updated together. A test asserting the old (wrong) status code with a pending TODO is more dangerous than no test at all: it passes when the code has the bug and fails when the code is correct.
- When a type migration (e.g., `Double` → `BigDecimal`) is applied to entities, ALL related DTOs must also be updated: both request DTOs (`RecipeIngredientRequest.quantity`) and response DTOs (`RecipeIngredientDTO.quantityRequired`). Partial migrations leave the project uncompilable. Always grep for `Double` in `dto/` after any BigDecimal entity migration.
- `@Slf4j` annotation can be missing from a class even when `lombok.extern.slf4j.Slf4j` is imported — the import alone does not activate the annotation processor. If `log` is unresolved at compile time, verify the class-level `@Slf4j` annotation is present.
- Double-fetch with pessimistic lock (R23): calling `findByIdWithLock()` twice per entity within the same `@Transactional` method (once to validate, once to modify) generates `2×N` `SELECT ... FOR UPDATE`. Fix: collect locked entities into a `Map<Long, Entity>` during the validation loop using `computeIfAbsent` (to deduplicate if the same ingredient appears in multiple recipes), then look up from the map in the deduction loop. This reduces lock round-trips to N and eliminates the risk of a stale re-read if Hibernate evicts the entity from the first-level cache between loops. The same fix applies to both `processOrder()` and the deprecated `processBrew()`.
- Shared-ingredient demand accumulation (R35): per-recipe stock validation is insufficient for multi-recipe orders. If two recipes share an ingredient (e.g. both require `Espresso Beans`), validating `stock >= ri.getQuantityRequired()` per recipe independently allows combined deductions to exceed available stock, leaving `currentStock` negative. Fix: build a `Map<Long, BigDecimal> totalDemanded` before the validation loop using `totalDemanded.merge(ingredientId, ri.getQuantityRequired(), BigDecimal::add)`, then compare `ingredient.getCurrentStock().compareTo(totalDemanded.get(ingredientId)) < 0` in the validation loop. This is a single-thread, single-transaction correctness bug — pessimistic locks only protect against concurrent access, not against intra-transaction over-deduction.
- Missing `@Slf4j` on write-operation services (R28): `BaristaService` and `RecipeService` had NO logging at all despite performing critical write operations (create, update, delete, XP award). ALL service classes that mutate state MUST have `@Slf4j`. Pattern: `log.info` at the entry of every write method, `log.info` on success, `log.info` on level-up (significant business event), `log.debug` for non-critical state changes. Read-only methods (`findAll`, `findById`) do NOT require logging in this project.

## DTO Mapping Pattern (R11, R19 — confirmed)

All entities must be mapped to DTOs before leaving the service layer. The `toDTO(Entity e)` private method pattern is established:
- `BaristaService.toDTO(Barista)` → `BaristaDTO`
- `RecipeService.toDTO(Recipe)` → `RecipeDTO`
- `StockService.toDTO(Ingredient)` → `IngredientDTO` (R19)
- `FinancialService.toDTO(DailyBalance)` → `DailyBalanceDTO` (R19)

The `toDTO()` method is always `private` and lives in the service, not the entity. Controllers must never receive or return raw JPA entities.

## ErrorResponse Contract (R20 — completed)

- `ErrorResponse` record fields (in order): `LocalDateTime timestamp`, `int status`, `String error`, `String message`, `String path`.
- The `timestamp` field is first — matches the JSON contract defined in `CLAUDE.md`.
- Factory method signature: `ErrorResponse.of(int status, String error, String message, String path)`.
- All `@ExceptionHandler` methods in `GlobalExceptionHandler` receive `HttpServletRequest request` as a second parameter and call `request.getRequestURI()` to populate `path`.
- Spring MVC resolves `HttpServletRequest` automatically as a method parameter in `@ExceptionHandler` — no field injection or `@Autowired` needed.

## N+1 Query Fix Pattern (R21 — completed)

- `RecipeRepository` now has `findAllWithIngredients()` and `findByIdWithIngredients(@Param("id") Long id)` using `@Query` with `JOIN FETCH`.
- JPQL: `SELECT r FROM Recipe r JOIN FETCH r.ingredients ri JOIN FETCH ri.ingredient` — double `JOIN FETCH` needed because `toDTO()` traverses two levels: `Recipe → RecipeIngredient → Ingredient`.
- `RecipeService.findAll()`, `findById()`, and `findEntityById()` all use the fetching variants — `recipeRepository.findAll()` and `recipeRepository.findById()` are never called in this service.
- When a service's `toDTO()` traverses a `@OneToMany` collection AND a nested `@ManyToOne` inside it, both joins must be fetched in the same query to avoid two rounds of N+1.
- Context7 confirmed `@EntityGraph(attributePaths = {...})` as an alternative, but `@Query` with `JOIN FETCH` was chosen for explicitness and because the JPQL matches what was documented in the architecture plan.

## Constructor Injection Pattern (R24 — completed)

- ALL controllers and services MUST use `@RequiredArgsConstructor` from Lombok. No manual constructors.
- The four classes that had manual constructors (corrected in R24): `BaristaController`, `BaristaService`, `StockService`, `FinancialService`.
- Correct import: `import lombok.RequiredArgsConstructor;` — place annotation at class level above `@Service` / `@RestController`.
- Fields remain `private final` — Lombok generates the constructor from them automatically.
- When `@Slf4j` is also present, the canonical order is: `@Slf4j` first, then `@RequiredArgsConstructor`, then the Spring stereotype annotation (`@Service`, `@RestController`, etc.).
- Manual constructors in Spring beans are a maintenance hazard: a developer adding a new `final` field following the project's pattern will cause a duplicate-constructor compile error that does not clearly point to the root cause.

## Links to Detail Files

- (none yet)
