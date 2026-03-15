package com.brewstack.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "ingredients", indexes = {
    @Index(name = "idx_ingredients_name", columnList = "name"),
    @Index(name = "idx_ingredients_stock_threshold", columnList = "current_stock, minimum_threshold")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 3)
    private BigDecimal currentStock;

    @Column(precision = 10, scale = 3)
    private BigDecimal minimumThreshold;

    @Column(nullable = false)
    private String unit;
}
