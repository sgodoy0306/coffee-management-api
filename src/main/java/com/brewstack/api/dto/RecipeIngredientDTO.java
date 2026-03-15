package com.brewstack.api.dto;

import java.math.BigDecimal;

public record RecipeIngredientDTO(
        String ingredientName,
        String unit,
        BigDecimal quantityRequired
) {}
