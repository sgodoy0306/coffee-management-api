package com.brewstack.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "recipes", indexes = {
    @Index(name = "idx_recipes_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer baseXpReward;

    @Column(name = "price", precision = 10, scale = 2, columnDefinition = "NUMERIC(10,2)")
    private BigDecimal price;

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients;
}
