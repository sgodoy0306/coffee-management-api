-- V4__add_not_null_constraints.sql
-- Add NOT NULL constraints to mandatory columns that were nullable in V1.
-- Bean Validation on DTOs protects the REST layer, but NOT NULL at the DB level
-- closes the contract for all write paths: batch jobs, DataInitializer, internal
-- service calls, and direct JDBC access.
--
-- Pre-condition: DataInitializer guarantees all existing rows have non-null values
-- for these columns, so the ALTER TABLE statements will succeed without USING clauses.

-- baristas.name: every barista must have a name
ALTER TABLE baristas ALTER COLUMN name SET NOT NULL;

-- ingredients.name: every ingredient must have a name
ALTER TABLE ingredients ALTER COLUMN name SET NOT NULL;

-- ingredients.unit: every ingredient must declare its unit of measure (g, ml, units, etc.)
ALTER TABLE ingredients ALTER COLUMN unit SET NOT NULL;

-- recipes.name: every recipe must have a name
ALTER TABLE recipes ALTER COLUMN name SET NOT NULL;

-- recipe_ingredients.quantity_required: every recipe-ingredient link must specify an amount
-- (column type is already NUMERIC(10,3) since V2__alter_stock_quantities_to_numeric.sql)
ALTER TABLE recipe_ingredients ALTER COLUMN quantity_required SET NOT NULL;
