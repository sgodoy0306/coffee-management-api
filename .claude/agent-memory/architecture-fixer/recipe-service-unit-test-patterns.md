---
name: RecipeService unit test patterns
description: Mockito stub patterns required when unit-testing RecipeService.createRecipe() and updateRecipe()
type: project
---

## RecipeService.createRecipe() — double save() pattern

`createRecipe()` calls `recipeRepository.save()` TWICE in the same transaction:
1. First save: persists the bare `Recipe` entity (no ingredients) to obtain the generated `id`.
2. Second save: persists the same `Recipe` after `setIngredients(links)` is called on it.

When mocking, use Mockito's varargs form to return different values per invocation:

```java
given(recipeRepository.save(any(Recipe.class))).willReturn(bareRecipe, recipeWithIngredients);
```

Verify both invocations:

```java
verify(recipeRepository, times(2)).save(any(Recipe.class));
```

The second return value must have the `RecipeIngredient` list populated so that `toDTO()` (which traverses `ri.getIngredient().getName()` and `ri.getIngredient().getUnit()`) does not throw NPE.

## RecipeService.updateRecipe() — mutable ingredient list required

`updateRecipe()` calls `recipe.getIngredients().clear()` followed by `addAll()`. If the `Recipe` fixture is initialized with `List.of()` (immutable), this throws `UnsupportedOperationException`.

Always initialize the ingredient list in `@BeforeEach` as a mutable `ArrayList`:

```java
latte.setIngredients(new ArrayList<>(List.of()));
// or, when there are existing elements:
latte.setIngredients(new ArrayList<>(List.of(existingLink)));
```

## updateRecipe with null ingredients — no ingredientRepository call

When `UpdateRecipeRequest.ingredients()` is `null`, the service's `if` block is skipped entirely. The stub for `ingredientRepository.findById()` must NOT be set (Mockito strict stubs will fail if a stub is registered but never called). Verify no-call explicitly:

```java
verify(ingredientRepository, never()).findById(any());
```

## findEntityById uses findByIdWithIngredients, not findById

Both `updateRecipe` and `deleteRecipe` resolve the entity via `findEntityById(id)`, which internally calls `recipeRepository.findByIdWithIngredients(id)`. Never stub `recipeRepository.findById()` in these test cases — the stub will be ignored and the test will fail with `RecipeNotFoundException`.
