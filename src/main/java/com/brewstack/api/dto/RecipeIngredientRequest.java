package com.brewstack.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RecipeIngredientRequest(
        @NotNull Long ingredientId,
        @Positive BigDecimal quantity
) {}
