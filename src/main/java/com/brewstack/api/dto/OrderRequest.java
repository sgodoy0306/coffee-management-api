package com.brewstack.api.dto;

import com.brewstack.api.model.OrderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "recipeIds must not be empty")
        List<Long> recipeIds,

        @NotNull(message = "baristaId must not be null")
        Long baristaId,

        // Nullable: BrewService defaults to DINE_IN for backward compatibility
        OrderType orderType
) {}
