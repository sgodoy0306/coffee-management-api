package com.brewstack.api.repository;

import com.brewstack.api.model.Ingredient;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Ingredient i WHERE i.id = :id")
    Optional<Ingredient> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT i FROM Ingredient i WHERE i.currentStock <= i.minimumThreshold")
    List<Ingredient> findLowStockIngredients();

    boolean existsByName(String name);

    Optional<Ingredient> findByName(String name);
}
