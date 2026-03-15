---
name: StockIntegrationTest patterns
description: Patterns and pitfalls discovered while implementing StockIntegrationTest (R60)
type: project
---

## PATCH support in TestRestTemplate (R60)

`java.net.HttpURLConnection` (the default HTTP factory in Spring) does NOT support the PATCH method — it throws `java.net.ProtocolException: Invalid HTTP method: PATCH`.

**Fix:** Add `httpclient5` (Apache HttpClient 5) as a `test` scope dependency. Spring Boot 3.2 BOM manages version **5.2.1**. When `httpclient5` is on the classpath, `TestRestTemplate` automatically switches to `HttpComponentsClientHttpRequestFactory`, which supports all HTTP methods including PATCH.

```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <scope>test</scope>
</dependency>
```

No version needed — Spring Boot BOM provides `5.2.1`.

## deleteAll() Order for Ingredient-touching Tests

`recipe_ingredients` has a FK to `ingredients` (constraint `fk_recipe_ingredients_ingredient`).
`DataInitializer` seeds 7 recipes that reference ingredients on startup.

Any `@BeforeEach` that calls `ingredientRepository.deleteAll()` MUST first call `recipeRepository.deleteAll()` to cascade-delete `recipe_ingredients`. Skipping this causes:

```
ERROR: update or delete on table "ingredients" violates foreign key constraint
       "fk_recipe_ingredients_ingredient" on table "recipe_ingredients"
```

Correct teardown order for any test class that cleans ingredients:
1. `recipeRepository.deleteAll()` — cascades to `recipe_ingredients`
2. `ingredientRepository.deleteAll()`

## Page<T> Deserialization with TestRestTemplate

`GET /api/stock` returns `Page<IngredientDTO>`. `Page` is an interface — `TestRestTemplate` cannot deserialize it directly. Pattern:

```java
ResponseEntity<Map> response = restTemplate.getForEntity("/api/stock", Map.class);
List<?> content = (List<?>) response.getBody().get("content");
Map<?, ?> item = (Map<?, ?>) content.get(0);
assertThat(item.get("name")).isEqualTo("Espresso Beans");
```

## Why: Explains both

- The `BrewIntegrationTest` already used this FK-order — StockIntegrationTest had to follow the same pattern.
- The `httpclient5` gap is silent until a PATCH endpoint is tested; adding it to `pom.xml` is a one-time project setup step that benefits any future PATCH test.
