package com.brewstack.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RestockRequest(
        @NotNull(message = "amount must not be null")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        BigDecimal amount
) {}
