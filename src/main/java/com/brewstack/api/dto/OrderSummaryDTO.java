package com.brewstack.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderSummaryDTO(
        List<String> brewedRecipes,
        BigDecimal totalRevenue,
        int totalOrders,
        long baristaXp,
        int baristaLevel
) {}
