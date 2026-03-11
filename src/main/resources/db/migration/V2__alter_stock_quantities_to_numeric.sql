-- V2__alter_stock_quantities_to_numeric.sql
-- Change stock quantity columns from FLOAT8 (IEEE 754, imprecise) to NUMERIC(10,3)
-- (exact decimal arithmetic, 3 decimal places for physical quantities like grams/ml).

ALTER TABLE ingredients
    ALTER COLUMN current_stock     TYPE NUMERIC(10,3) USING current_stock::NUMERIC(10,3),
    ALTER COLUMN minimum_threshold TYPE NUMERIC(10,3) USING minimum_threshold::NUMERIC(10,3);

ALTER TABLE recipe_ingredients
    ALTER COLUMN quantity_required TYPE NUMERIC(10,3) USING quantity_required::NUMERIC(10,3);
