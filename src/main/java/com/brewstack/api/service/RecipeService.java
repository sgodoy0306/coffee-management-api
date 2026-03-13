package com.brewstack.api.service;

import com.brewstack.api.dto.CreateRecipeRequest;
import com.brewstack.api.dto.RecipeDTO;
import com.brewstack.api.dto.RecipeIngredientDTO;
import com.brewstack.api.exception.IngredientNotFoundException;
import com.brewstack.api.exception.RecipeNotFoundException;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.model.Recipe;
import com.brewstack.api.model.RecipeIngredient;
import com.brewstack.api.repository.IngredientRepository;
import com.brewstack.api.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;

    public List<RecipeDTO> findAll() {
        return recipeRepository.findAllWithIngredients().stream().map(this::toDTO).toList();
    }

    public RecipeDTO findById(Long id) {
        return toDTO(recipeRepository.findByIdWithIngredients(id)
                .orElseThrow(() -> new RecipeNotFoundException(id)));
    }

    @Transactional
    public RecipeDTO createRecipe(CreateRecipeRequest request) {
        log.info("Creating new recipe with name='{}'", request.name());
        Recipe recipe = new Recipe();
        recipe.setName(request.name());
        recipe.setBaseXpReward(request.baseXpReward());
        recipe.setPrice(request.price());
        recipe.setImageUrl(request.imageUrl());
        Recipe saved = recipeRepository.save(recipe);

        List<RecipeIngredient> links = request.ingredients().stream().map(req -> {
            Ingredient ingredient = ingredientRepository.findById(req.ingredientId())
                    .orElseThrow(() -> new IngredientNotFoundException(req.ingredientId()));
            return new RecipeIngredient(null, saved, ingredient, req.quantity());
        }).toList();

        saved.setIngredients(links);
        RecipeDTO created = toDTO(recipeRepository.save(saved));
        log.info("Recipe created successfully with id={}", created.id());
        return created;
    }

    private RecipeDTO toDTO(Recipe recipe) {
        List<RecipeIngredientDTO> ingredients = recipe.getIngredients().stream()
                .map(ri -> new RecipeIngredientDTO(
                        ri.getIngredient().getName(),
                        ri.getIngredient().getUnit(),
                        ri.getQuantityRequired()))
                .toList();
        return new RecipeDTO(recipe.getId(), recipe.getName(), recipe.getBaseXpReward(), recipe.getPrice(), recipe.getImageUrl(), ingredients);
    }

    private Recipe findEntityById(Long id) {
        return recipeRepository.findByIdWithIngredients(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    @Transactional
    public RecipeDTO updateRecipe(Long id, String name, Integer baseXpReward, BigDecimal price, String imageUrl) {
        log.info("Updating recipe id={} with name='{}'", id, name);
        Recipe recipe = findEntityById(id);
        recipe.setName(name);
        recipe.setBaseXpReward(baseXpReward);
        recipe.setPrice(price);
        recipe.setImageUrl(imageUrl);
        return toDTO(recipeRepository.save(recipe));
    }

    public void deleteRecipe(Long id) {
        log.info("Deleting recipe id={}", id);
        if (!recipeRepository.existsById(id)) {
            throw new RecipeNotFoundException(id);
        }
        recipeRepository.deleteById(id);
        log.info("Recipe id={} deleted successfully", id);
    }
}
