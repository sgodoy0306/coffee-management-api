package com.brewstack.api.repository;

import com.brewstack.api.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    boolean existsByName(String name);
}
