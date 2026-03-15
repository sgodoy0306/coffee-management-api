package com.brewstack.api.dto;

import java.math.BigDecimal;

public record IngredientDTO(
        Long id,
        String name,
        BigDecimal currentStock,
        BigDecimal minimumThreshold,
        String unit
) {}
