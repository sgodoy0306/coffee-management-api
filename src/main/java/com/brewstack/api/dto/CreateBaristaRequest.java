package com.brewstack.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBaristaRequest(
        @NotBlank String name
) {
}
