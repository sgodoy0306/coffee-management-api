-- V3__add_indexes.sql
-- Add indexes on non-PK columns used in frequent WHERE clauses and ORDER BY operations.
-- These columns perform full-table scans without indexes; this migration eliminates that overhead.

-- ingredients.name: used by existsByName() and findByName() in DataInitializer and validation
CREATE INDEX IF NOT EXISTS idx_ingredients_name ON ingredients(name);

-- ingredients(current_stock, minimum_threshold): used by findLowStockIngredients()
-- with WHERE current_stock <= minimum_threshold
CREATE INDEX IF NOT EXISTS idx_ingredients_stock_threshold ON ingredients(current_stock, minimum_threshold);

-- recipes.name: used by existsByName() in DataInitializer and recipe validation
CREATE INDEX IF NOT EXISTS idx_recipes_name ON recipes(name);

-- daily_balances.date: used by findAllByOrderByDateDesc() with ORDER BY date DESC
CREATE INDEX IF NOT EXISTS idx_daily_balances_date ON daily_balances(date);
