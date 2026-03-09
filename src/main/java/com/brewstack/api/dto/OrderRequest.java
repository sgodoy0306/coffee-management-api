package com.brewstack.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "recipeIds must not be empty")
        List<Long> recipeIds,

        @NotNull(message = "baristaId must not be null")
        Long baristaId
) {}
