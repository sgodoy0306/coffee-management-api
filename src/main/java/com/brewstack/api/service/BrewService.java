package com.brewstack.api.service;

import com.brewstack.api.exception.BaristaNotFoundException;
import com.brewstack.api.exception.InsufficientStockException;
import com.brewstack.api.exception.RecipeNotFoundException;
import com.brewstack.api.model.Barista;
import com.brewstack.api.model.DailyBalance;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.model.Recipe;
import com.brewstack.api.model.RecipeIngredient;
import com.brewstack.api.repository.BaristaRepository;
import com.brewstack.api.repository.DailyBalanceRepository;
import com.brewstack.api.repository.IngredientRepository;
import com.brewstack.api.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BrewService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final DailyBalanceRepository dailyBalanceRepository;
    private final BaristaRepository baristaRepository;

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

    /**
     * Processes a multi-recipe order for a barista.
     * Returns a map with baristaXp and baristaLevel after awarding XP.
     */
    @Transactional
    public Map<String, Object> processOrder(List<Long> recipeIds, Long baristaId) {
        Barista barista = baristaRepository.findById(baristaId)
                .orElseThrow(() -> new BaristaNotFoundException(baristaId));

        List<Recipe> recipes = new ArrayList<>();
        for (Long recipeId : recipeIds) {
            recipes.add(recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new RecipeNotFoundException(recipeId)));
        }

        // Validate stock for all recipes before deducting anything
        for (Recipe recipe : recipes) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                Ingredient ingredient = ri.getIngredient();
                if (ingredient.getCurrentStock() < ri.getQuantityRequired()) {
                    throw new InsufficientStockException(ingredient.getName());
                }
            }
        }

        // Deduct stock and accumulate XP
        long totalXpGained = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Recipe recipe : recipes) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                Ingredient ingredient = ri.getIngredient();
                ingredient.setCurrentStock(ingredient.getCurrentStock() - ri.getQuantityRequired());
                ingredientRepository.save(ingredient);
            }
            totalXpGained += recipe.getBaseXpReward() != null ? recipe.getBaseXpReward() : 10;
            totalRevenue = totalRevenue.add(recipe.getPrice() != null ? recipe.getPrice() : BigDecimal.ZERO);
        }

        // Update daily balance
        LocalDate today = LocalDate.now();
        DailyBalance balance = dailyBalanceRepository.findById(today)
                .orElse(new DailyBalance(today, BigDecimal.ZERO, 0));
        balance.setTotalRevenue(balance.getTotalRevenue().add(totalRevenue));
        balance.setTotalOrders(balance.getTotalOrders() + recipeIds.size());
        dailyBalanceRepository.save(balance);

        // Award XP to barista
        barista.setTotalXp(barista.getTotalXp() + totalXpGained);
        int newLevel = (int) Math.floor(Math.sqrt(barista.getTotalXp() / 100.0)) + 1;
        barista.setLevel(newLevel);
        baristaRepository.save(barista);

        return Map.of(
                "baristaXp", barista.getTotalXp(),
                "baristaLevel", barista.getLevel(),
                "totalRevenue", totalRevenue,
                "totalOrders", recipeIds.size()
        );
    }
}
