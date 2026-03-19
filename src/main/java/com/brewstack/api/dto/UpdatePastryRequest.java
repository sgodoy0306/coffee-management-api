package com.brewstack.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdatePastryRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        String description,

        @NotNull(message = "price must not be null")
        @DecimalMin(value = "0.01", message = "price must be greater than 0")
        BigDecimal price,

        boolean available,

        String imageUrl
) {}
