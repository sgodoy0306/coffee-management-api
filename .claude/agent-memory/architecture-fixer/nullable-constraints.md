---
name: nullable-constraints-pattern
description: R45/R51 — How to enforce NOT NULL at both the JPA entity layer and the DB layer via Flyway, and why Bean Validation alone is insufficient
type: project
---

## Rule: Mandatory fields require TWO enforcement points

Bean Validation (`@NotBlank`, `@NotNull` on DTO records) protects only the REST layer. It does NOT cover:
- Batch import jobs
- `DataInitializer` with partial failure
- Tests that construct entities directly
- Internal service-to-service calls bypassing the controller

**Why:** `DailyBalance.totalRevenue` / `totalOrders` already had `@Column(nullable = false)` and `NOT NULL` in V1. The inconsistency with `Barista.name`, `Ingredient.name`, `Ingredient.unit`, `Recipe.name`, and `RecipeIngredient.quantityRequired` was a clear architectural gap.

**How to apply:** Whenever a field is conceptually mandatory (cannot be null for a valid domain object), apply BOTH:
1. `@Column(nullable = false)` on the entity field — documents intent in the domain model
2. A new Flyway migration with `ALTER TABLE ... ALTER COLUMN ... SET NOT NULL` — enforces at the DB level

## Completed fix (R45 + R51)

Flyway migration history at time of fix:
- `V1__init.sql` — initial schema (nullable columns were the bug)
- `V2__alter_stock_quantities_to_numeric.sql` — FLOAT8 → NUMERIC(10,3)
- `V3__add_indexes.sql` — performance indexes
- `V4__add_not_null_constraints.sql` — **this fix** (ALTER TABLE ... SET NOT NULL)

Entity fields updated with `@Column(nullable = false)`:
- `Barista.name`
- `Ingredient.name`
- `Ingredient.unit`
- `Recipe.name`
- `RecipeIngredient.quantityRequired` (merged into existing `@Column(nullable = false, precision = 10, scale = 3)`)

## Important: pre-condition for SET NOT NULL migrations

`ALTER TABLE ... SET NOT NULL` will fail at execution time if any existing row has a NULL value in that column. Before writing the migration, verify that:
- `DataInitializer` always populates those fields for seed data
- No previous migration could have inserted NULL values

In this project, `DataInitializer` guarantees all seed rows have valid values, so no `UPDATE` statements are needed before the `ALTER TABLE`.

## When NOT to add nullable = false

- FK columns handled via `@ManyToOne(optional = false)` — the JPA constraint is already enforced at the association level; the FK column itself gets `NOT NULL` from the join column definition.
- `@Id` fields — always NOT NULL by definition (primary key constraint).
- Optional fields by design (e.g., `Recipe.imageUrl` — an image URL is genuinely optional).
