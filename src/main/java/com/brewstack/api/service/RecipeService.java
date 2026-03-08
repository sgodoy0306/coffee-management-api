package com.brewstack.api.service;

import com.brewstack.api.dto.RecipeDTO;
import com.brewstack.api.dto.RecipeIngredientDTO;
import com.brewstack.api.exception.RecipeNotFoundException;
import com.brewstack.api.model.Recipe;
import com.brewstack.api.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;

    public List<RecipeDTO> findAll() {
        return recipeRepository.findAll().stream().map(this::toDTO).toList();
    }

    public RecipeDTO findById(Long id) {
        return toDTO(recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id)));
    }

    private RecipeDTO toDTO(Recipe recipe) {
        List<RecipeIngredientDTO> ingredients = recipe.getIngredients().stream()
                .map(ri -> new RecipeIngredientDTO(
                        ri.getIngredient().getName(),
                        ri.getIngredient().getUnit(),
                        ri.getQuantityRequired()))
                .toList();
        return new RecipeDTO(recipe.getId(), recipe.getName(), recipe.getBaseXpReward(), recipe.getPrice(), ingredients);
    }

    private Recipe findEntityById(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    @Transactional
    public RecipeDTO updateRecipe(Long id, String name, Integer baseXpReward, BigDecimal price) {
        Recipe recipe = findEntityById(id);
        recipe.setName(name);
        recipe.setBaseXpReward(baseXpReward);
        recipe.setPrice(price);
        return toDTO(recipeRepository.save(recipe));
    }

    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new RecipeNotFoundException(id);
        }
        recipeRepository.deleteById(id);
    }
}
