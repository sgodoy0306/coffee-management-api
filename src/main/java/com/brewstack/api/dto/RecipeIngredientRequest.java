package com.brewstack.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RecipeIngredientRequest(
        @NotBlank String ingredientName,
        @Positive Double quantity
) {}
