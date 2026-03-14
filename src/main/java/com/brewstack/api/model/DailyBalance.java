package com.brewstack.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_balances", indexes = {
    @Index(name = "idx_daily_balances_date", columnList = "date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyBalance {

    @Id
    private LocalDate date;

    @Column(nullable = false, precision = 10, scale = 2, columnDefinition = "NUMERIC(10,2)")
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer totalOrders = 0;
}
