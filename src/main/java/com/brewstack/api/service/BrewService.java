package com.brewstack.api.service;

import com.brewstack.api.exception.InsufficientStockException;
import com.brewstack.api.exception.RecipeNotFoundException;
import com.brewstack.api.model.DailyBalance;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.model.Recipe;
import com.brewstack.api.model.RecipeIngredient;
import com.brewstack.api.repository.DailyBalanceRepository;
import com.brewstack.api.repository.IngredientRepository;
import com.brewstack.api.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BrewService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final DailyBalanceRepository dailyBalanceRepository;

    @Transactional
    public void processBrew(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        for (RecipeIngredient recipeIngredient : recipe.getIngredients()) {
            Ingredient ingredient = recipeIngredient.getIngredient();
            if (ingredient.getCurrentStock() < recipeIngredient.getQuantityRequired()) {
                throw new InsufficientStockException(ingredient.getName());
            }
        }

        for (RecipeIngredient recipeIngredient : recipe.getIngredients()) {
            Ingredient ingredient = recipeIngredient.getIngredient();
            ingredient.setCurrentStock(ingredient.getCurrentStock() - recipeIngredient.getQuantityRequired());
            ingredientRepository.save(ingredient);
        }

        LocalDate today = LocalDate.now();
        DailyBalance balance = dailyBalanceRepository.findById(today)
                .orElse(new DailyBalance(today, BigDecimal.ZERO, 0));

        BigDecimal recipePrice = recipe.getPrice() != null ? recipe.getPrice() : BigDecimal.ZERO;
        balance.setTotalRevenue(balance.getTotalRevenue().add(recipePrice));
        balance.setTotalOrders(balance.getTotalOrders() + 1);

        dailyBalanceRepository.save(balance);
    }
}
