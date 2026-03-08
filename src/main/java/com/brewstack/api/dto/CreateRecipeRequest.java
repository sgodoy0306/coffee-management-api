package com.brewstack.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateRecipeRequest(
        @NotBlank String name,
        @NotNull @Min(0) Integer baseXpReward,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotEmpty @Valid List<RecipeIngredientRequest> ingredients
) {}
