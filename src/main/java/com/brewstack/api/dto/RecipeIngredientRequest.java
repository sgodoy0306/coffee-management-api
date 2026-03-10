package com.brewstack.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecipeIngredientRequest(
        @NotNull Long ingredientId,
        @Positive Double quantity
) {}
