package com.brewstack.api.service;

import com.brewstack.api.dto.OrderRequest;
import com.brewstack.api.dto.OrderSummaryDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Transactional
    public OrderSummaryDTO processOrder(OrderRequest request) {
        Barista barista = baristaRepository.findById(request.baristaId())
                .orElseThrow(() -> new BaristaNotFoundException(request.baristaId()));

        // Resolve all recipes first so we fail fast on missing IDs
        List<Recipe> recipes = new ArrayList<>();
        for (Long recipeId : request.recipeIds()) {
            recipes.add(recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new RecipeNotFoundException(recipeId)));
        }

        // Validate stock for every item before touching anything (atomicity).
        // findByIdWithLock issues SELECT ... FOR UPDATE, preventing concurrent
        // transactions from reading the same stock value and both passing validation.
        for (Recipe recipe : recipes) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                Ingredient ingredient = ingredientRepository
                        .findByIdWithLock(ri.getIngredient().getId())
                        .orElseThrow(() -> new InsufficientStockException(ri.getIngredient().getName()));
                if (ingredient.getCurrentStock() < ri.getQuantityRequired()) {
                    throw new InsufficientStockException(ingredient.getName());
                }
            }
        }

        // All checks passed — deduct stock and accumulate revenue + XP
        BigDecimal orderRevenue = BigDecimal.ZERO;
        long xpGained = 0L;
        List<String> brewedNames = new ArrayList<>();

        for (Recipe recipe : recipes) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                Ingredient ingredient = ingredientRepository
                        .findByIdWithLock(ri.getIngredient().getId())
                        .orElseThrow(() -> new InsufficientStockException(ri.getIngredient().getName()));
                ingredient.setCurrentStock(ingredient.getCurrentStock() - ri.getQuantityRequired());
                ingredientRepository.save(ingredient);
            }
            BigDecimal price = recipe.getPrice() != null ? recipe.getPrice() : BigDecimal.ZERO;
            orderRevenue = orderRevenue.add(price);
            xpGained += recipe.getBaseXpReward() != null ? recipe.getBaseXpReward() : 0;
            brewedNames.add(recipe.getName());
        }

        // Update daily balance
        LocalDate today = LocalDate.now();
        DailyBalance balance = dailyBalanceRepository.findById(today)
                .orElse(new DailyBalance(today, BigDecimal.ZERO, 0));
        balance.setTotalRevenue(balance.getTotalRevenue().add(orderRevenue));
        balance.setTotalOrders(balance.getTotalOrders() + recipes.size());
        dailyBalanceRepository.save(balance);

        // Grant XP to barista
        barista.setTotalXp(barista.getTotalXp() + xpGained);
        int newLevel = (int) Math.floor(Math.sqrt(barista.getTotalXp() / 100.0)) + 1;
        barista.setLevel(newLevel);
        baristaRepository.save(barista);

        return new OrderSummaryDTO(brewedNames, orderRevenue, recipes.size(), barista.getTotalXp(), newLevel);
    }
}
