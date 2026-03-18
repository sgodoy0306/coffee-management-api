package com.brewstack.api.dto;

import com.brewstack.api.model.OrderType;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "recipeIds must not be empty")
        List<Long> recipeIds,

        // Nullable: when null the order is processed without assigning a barista.
        Long baristaId,

        // Nullable: BrewService defaults to DINE_IN for backward compatibility
        OrderType orderType
) {}
