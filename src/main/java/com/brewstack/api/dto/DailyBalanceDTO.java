package com.brewstack.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyBalanceDTO(
        LocalDate date,
        BigDecimal totalRevenue,
        Integer totalOrders
) {}
