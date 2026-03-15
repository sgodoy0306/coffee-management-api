-- V1__init.sql
-- Initial schema for the Coffee Management API.
-- Reflects JPA entities as Hibernate would generate them with ddl-auto=create.
-- Compatible with PostgreSQL 16.

-- ============================================================
-- Table: baristas
-- Entity: com.brewstack.api.model.Barista
-- PK strategy: GenerationType.IDENTITY → BIGSERIAL
-- ============================================================
CREATE TABLE IF NOT EXISTS baristas (
    id         BIGSERIAL    NOT NULL,
    name       VARCHAR(255),
    level      INTEGER      NOT NULL DEFAULT 1,
    total_xp   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_baristas PRIMARY KEY (id)
);

-- ============================================================
-- Table: ingredients
-- Entity: com.brewstack.api.model.Ingredient
-- PK strategy: GenerationType.IDENTITY → BIGSERIAL
-- ============================================================
CREATE TABLE IF NOT EXISTS ingredients (
    id                  BIGSERIAL      NOT NULL,
    name                VARCHAR(255),
    current_stock       FLOAT8,
    minimum_threshold   FLOAT8,
    unit                VARCHAR(255),
    CONSTRAINT pk_ingredients PRIMARY KEY (id)
);

-- ============================================================
-- Table: recipes
-- Entity: com.brewstack.api.model.Recipe
-- PK strategy: GenerationType.IDENTITY → BIGSERIAL
-- ============================================================
CREATE TABLE IF NOT EXISTS recipes (
    id              BIGSERIAL       NOT NULL,
    name            VARCHAR(255),
    base_xp_reward  INTEGER,
    price           NUMERIC(10, 2),
    image_url       VARCHAR(255),
    CONSTRAINT pk_recipes PRIMARY KEY (id)
);

-- ============================================================
-- Table: recipe_ingredients
-- Entity: com.brewstack.api.model.RecipeIngredient
-- PK strategy: GenerationType.IDENTITY → BIGSERIAL
-- FK: recipe_id → recipes(id), ingredient_id → ingredients(id)
-- ============================================================
CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id                  BIGSERIAL   NOT NULL,
    recipe_id           BIGINT      NOT NULL,
    ingredient_id       BIGINT      NOT NULL,
    quantity_required   FLOAT8,
    CONSTRAINT pk_recipe_ingredients PRIMARY KEY (id),
    CONSTRAINT fk_recipe_ingredients_recipe
        FOREIGN KEY (recipe_id)     REFERENCES recipes(id),
    CONSTRAINT fk_recipe_ingredients_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
);

-- ============================================================
-- Table: daily_balances
-- Entity: com.brewstack.api.model.DailyBalance
-- PK: date (LocalDate → DATE), no auto-generated sequence
-- ============================================================
CREATE TABLE IF NOT EXISTS daily_balances (
    date            DATE            NOT NULL,
    total_revenue   NUMERIC(10, 2)  NOT NULL DEFAULT 0.00,
    total_orders    INTEGER         NOT NULL DEFAULT 0,
    CONSTRAINT pk_daily_balances PRIMARY KEY (date)
);
