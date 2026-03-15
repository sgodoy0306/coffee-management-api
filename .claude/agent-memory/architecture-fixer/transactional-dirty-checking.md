---
name: Redundant save() inside @Transactional — Dirty Checking Pattern
description: R49 pattern — calling repository.save() on a managed entity inside a @Transactional method is redundant because Hibernate dirty-checking flushes all changes automatically at commit.
type: feedback
---

## Rule

Never call `repository.save(entity)` on a managed entity inside a `@Transactional` method when the only purpose is to persist a field mutation made via a setter. Hibernate's first-level cache tracks all changes to managed entities and flushes them automatically when the transaction commits — the explicit `save()` is pure noise.

**Why:** The redundant call adds a misleading signal: it implies that without `save()` the change would be lost (it would not). It also creates maintenance risk — a developer might remove the `save()` call in future assuming the change is intentional "no-op territory", or conversely add more `save()` calls for other setters following the bad pattern. Dirty checking is a core Hibernate contract for managed entities within a transaction.

**How to apply:** When reviewing service methods annotated with `@Transactional`, check every `repository.save(entity)` call:
- If the entity was returned by a repository `find*` call within the same transaction, it is managed — the `save()` is redundant and must be removed.
- If the entity is newly constructed (`new Entity(...)`) within the method, it is transient — `save()` is required to persist it (e.g., `dailyBalanceRepository.save(balance)` in `BrewService` is correct).
- Exception: `save()` is always required when saving a **new** entity (transient state) to trigger `INSERT`, even inside `@Transactional`.

## Where this was found

- `BrewService.processOrder()` — deduction loop called `ingredientRepository.save(ingredient)` after `ingredient.setCurrentStock(...)`. The `ingredient` entity was locked and returned by `ingredientRepository.findByIdWithLock()` within the same `@Transactional` method — fully managed. The `save()` was removed in R49.
- Note: `ingredientRepository` remained in the class because `findByIdWithLock()` is still used in the validation loop. Before removing a repository field, always scan the entire class for other usages.
