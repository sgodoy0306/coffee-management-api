package com.brewstack.api.repository;

import com.brewstack.api.model.Recipe;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    boolean existsByName(String name);

    @Query("SELECT r FROM Recipe r JOIN FETCH r.ingredients ri JOIN FETCH ri.ingredient")
    List<Recipe> findAllWithIngredients();

    @Query("SELECT r FROM Recipe r JOIN FETCH r.ingredients ri JOIN FETCH ri.ingredient WHERE r.id = :id")
    Optional<Recipe> findByIdWithIngredients(@Param("id") Long id);
}
