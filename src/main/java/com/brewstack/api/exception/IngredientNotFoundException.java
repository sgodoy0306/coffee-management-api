package com.brewstack.api.exception;

public class IngredientNotFoundException extends RuntimeException {

    public IngredientNotFoundException(Long id) {
        super("Ingredient not found with id: " + id);
    }

    public IngredientNotFoundException(String name) {
        super("Ingredient not found with name: " + name);
    }
}
