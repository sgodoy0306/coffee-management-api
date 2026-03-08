package com.brewstack.api.repository;

import com.brewstack.api.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    @Query("SELECT i FROM Ingredient i WHERE i.currentStock <= i.minimumThreshold")
    List<Ingredient> findLowStockIngredients();

    boolean existsByName(String name);

    Optional<Ingredient> findByName(String name);
}
