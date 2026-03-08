package com.brewstack.api.dto;

public record RecipeIngredientDTO(
        String ingredientName,
        String unit,
        Double quantityRequired
) {}
