-- V6__add_description_to_recipes_and_image_url_to_pastries.sql
-- Adds description column to recipes and image_url column to pastries.

ALTER TABLE recipes
    ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE pastries
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(255);
