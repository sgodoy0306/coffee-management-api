package com.brewstack.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PracticeRequest(
        @Min(1) @Max(10) int rating
) {
}
