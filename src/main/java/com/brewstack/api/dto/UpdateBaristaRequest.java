package com.brewstack.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateBaristaRequest(
        @NotBlank String name
) {}
