package com.brewstack.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateIngredientRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        @NotNull(message = "currentStock must not be null")
        @DecimalMin(value = "0.0", inclusive = true, message = "currentStock must be >= 0")
        BigDecimal currentStock,

        @NotNull(message = "minimumThreshold must not be null")
        @DecimalMin(value = "0.0", inclusive = true, message = "minimumThreshold must be >= 0")
        BigDecimal minimumThreshold,

        @NotBlank(message = "unit must not be blank")
        String unit
) {}
