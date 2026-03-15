---
name: Immutable collection + Hibernate merge (R61)
description: Java Stream .toList() returns an immutable list; assigning it to a JPA @OneToMany field and then calling repository.save() on an already-persistent entity triggers merge(), which calls .clear() on the collection and throws UnsupportedOperationException.
type: feedback
---

## Rule
Never assign `.stream().toList()` (Java 16+) as the collection field of a JPA entity that will be merged (i.e. already has an ID when `save()` is called). Always use a mutable `ArrayList`.

**Why:** `Stream.toList()` returns an unmodifiable list (backed by `ImmutableCollections`). When Hibernate performs a merge on a persistent entity, its `CollectionType.replaceElements()` calls `.clear()` on the current collection snapshot, which throws `UnsupportedOperationException` for any `ImmutableCollections` type.

**How to apply:** In any service method that:
1. Saves an entity to get an ID (first `save()`), then
2. Populates a collection field with the new ID in scope, then
3. Calls `save()` again (triggering merge)

Replace the `.toList()` terminal with `.collect(Collectors.toCollection(ArrayList::new))` or `new ArrayList<>(stream.toList())`.

## Affected file (fixed in R61)
`RecipeService.createRecipe()` — lines 50-54. Changed from `.toList()` to `Collectors.toCollection(ArrayList::new)`.

## Related test fixture rule
In integration test `@BeforeEach`, never use `List.of(...)` to populate a `@OneToMany` collection before `repository.save()`. Use `new ArrayList<>(List.of(...))`. The same hibernate merge mechanism can operate on the cached entity within the same test's Hibernate session context.

## Pattern for detection
Grep: `\.toList\(\);\s*\n\s*\w+\.set[A-Z]\w+\(` — a `.toList()` result immediately assigned to a setter call. If the entity has an `@Id` field already populated at that point, a merge will occur on the second `save()`.
