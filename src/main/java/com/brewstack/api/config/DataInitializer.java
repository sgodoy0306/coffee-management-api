package com.brewstack.api.config;

import com.brewstack.api.model.Ingredient;
import com.brewstack.api.model.Recipe;
import com.brewstack.api.model.RecipeIngredient;
import com.brewstack.api.repository.IngredientRepository;
import com.brewstack.api.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.brewstack.api.exception.IngredientNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;

    @Override
    public void run(String... args) {
        seedIngredientIfAbsent("Espresso Beans",   5000.0,  500.0, "grams");
        seedIngredientIfAbsent("Whole Milk",       10000.0, 1000.0, "ml");
        seedIngredientIfAbsent("Matcha Powder",    500.0,   100.0, "grams");
        seedIngredientIfAbsent("Chocolate Powder", 1000.0,  150.0, "grams");
        seedIngredientIfAbsent("Oat Milk",         8000.0,  1000.0, "ml");
        seedIngredientIfAbsent("Water",            10000.0, 2000.0, "ml");
        seedIngredientIfAbsent("Ice",              500.0,   100.0, "cubes");

        seedRecipeIfAbsent("Espresso",    10, new BigDecimal("3.50"), List.of(
                ri("Espresso Beans", 18.0)
        ));
        seedRecipeIfAbsent("Latte",       25, new BigDecimal("5.50"), List.of(
                ri("Espresso Beans", 18.0),
                ri("Whole Milk",     200.0)
        ));
        seedRecipeIfAbsent("Flat White",  30, new BigDecimal("5.00"), List.of(
                ri("Espresso Beans", 18.0),
                ri("Whole Milk",     150.0)
        ));
        seedRecipeIfAbsent("Cappuccino",  25, new BigDecimal("5.00"), List.of(
                ri("Espresso Beans", 18.0),
                ri("Whole Milk",     150.0)
        ));
        seedRecipeIfAbsent("Matcha Latte", 35, new BigDecimal("6.50"), List.of(
                ri("Matcha Powder", 5.0),
                ri("Oat Milk",      200.0)
        ));
        seedRecipeIfAbsent("Mocha",       30, new BigDecimal("6.00"), List.of(
                ri("Espresso Beans",    18.0),
                ri("Chocolate Powder",  15.0),
                ri("Whole Milk",        150.0)
        ));
        seedRecipeIfAbsent("Iced Americano", 15, new BigDecimal("4.50"), List.of(
                ri("Espresso Beans", 18.0),
                ri("Water",          200.0),
                ri("Ice",            3.0)
        ));
    }

    private void seedIngredientIfAbsent(String name, double currentStock, double minimumThreshold, String unit) {
        if (!ingredientRepository.existsByName(name)) {
            ingredientRepository.save(new Ingredient(null, name, currentStock, minimumThreshold, unit));
        }
    }

    private void seedRecipeIfAbsent(String name, int xp, BigDecimal price, List<RecipeIngredient> links) {
        if (recipeRepository.existsByName(name)) return;
        Recipe recipe = recipeRepository.save(new Recipe(null, name, xp, price, null));
        List<RecipeIngredient> saved = new ArrayList<>();
        for (RecipeIngredient link : links) {
            link.setRecipe(recipe);
            saved.add(link);
        }
        recipe.setIngredients(saved);
        recipeRepository.save(recipe);
    }

    /** Builds a RecipeIngredient stub — recipe reference is set later in seedRecipeIfAbsent. */
    private RecipeIngredient ri(String ingredientName, double quantity) {
        Ingredient ingredient = ingredientRepository.findByName(ingredientName)
                .orElseThrow(() -> new IngredientNotFoundException(ingredientName));
        return new RecipeIngredient(null, null, ingredient, quantity);
    }

}
