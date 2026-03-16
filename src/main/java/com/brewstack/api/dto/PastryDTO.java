package com.brewstack.api.dto;

import java.math.BigDecimal;

public record PastryDTO(
        Long id,
        String name,
        String description,
        BigDecimal price,
        boolean available
) {}
