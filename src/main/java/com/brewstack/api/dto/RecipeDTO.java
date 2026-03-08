package com.brewstack.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecipeDTO(
        Long id,
        String name,
        Integer baseXpReward,
        BigDecimal price,
        List<RecipeIngredientDTO> ingredients
) {}
