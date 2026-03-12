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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrewService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final DailyBalanceRepository dailyBalanceRepository;
    private final BaristaRepository baristaRepository;

    /**
     * @deprecated This endpoint is deprecated. Use {@link #processOrder(OrderRequest)} instead.
     *             This method does NOT award XP to any barista.
     *             Retained for backwards compatibility only.
     */
    @Deprecated
    @Transactional
    public void processBrew(Long recipeId) {
        log.warn("processBrew called for recipeId={}. This method is deprecated. Use processOrder instead.", recipeId);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        // Validate all stock first using pessimistic write lock (same atomicity guarantee as processOrder).
        // findByIdWithLock issues SELECT ... FOR UPDATE, preventing race conditions under concurrent requests.
        // Locked references are stored in a Map so the deduction loop reuses the same in-memory objects
        // instead of issuing a second SELECT ... FOR UPDATE per ingredient (R23 fix).
        Map<Long, Ingredient> lockedIngredients = new HashMap<>();
        for (RecipeIngredient recipeIngredient : recipe.getIngredients()) {
            Long ingredientId = recipeIngredient.getIngredient().getId();
            Ingredient ingredient = ingredientRepository
                    .findByIdWithLock(ingredientId)
                    .orElseThrow(() -> new InsufficientStockException(recipeIngredient.getIngredient().getName()));
            lockedIngredients.put(ingredientId, ingredient);
            if (ingredient.getCurrentStock().compareTo(recipeIngredient.getQuantityRequired()) < 0) {
                throw new InsufficientStockException(ingredient.getName());
            }
        }

        // All checks passed — deduct stock. No XP is awarded (deprecated/legacy behaviour).
        // Reuse the locked references from the validation loop — no additional SELECT FOR UPDATE issued.
        for (RecipeIngredient recipeIngredient : recipe.getIngredients()) {
            Ingredient ingredient = lockedIngredients.get(recipeIngredient.getIngredient().getId());
            ingredient.setCurrentStock(ingredient.getCurrentStock().subtract(recipeIngredient.getQuantityRequired()));
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
        // Locked references are stored in a Map keyed by ingredient ID so the deduction
        // loop can reuse the same in-memory objects without issuing a second
        // SELECT ... FOR UPDATE per ingredient (R23 fix: reduces 2×N locks to N locks).
        Map<Long, Ingredient> lockedIngredients = new HashMap<>();
        for (Recipe recipe : recipes) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                Long ingredientId = ri.getIngredient().getId();
                Ingredient ingredient = lockedIngredients.computeIfAbsent(ingredientId,
                        id -> ingredientRepository
                                .findByIdWithLock(id)
                                .orElseThrow(() -> new InsufficientStockException(ri.getIngredient().getName())));
                if (ingredient.getCurrentStock().compareTo(ri.getQuantityRequired()) < 0) {
                    throw new InsufficientStockException(ingredient.getName());
                }
            }
        }

        // All checks passed — deduct stock and accumulate revenue + XP.
        // Reuse the locked references from the validation map — no additional SELECT FOR UPDATE issued.
        BigDecimal orderRevenue = BigDecimal.ZERO;
        long xpGained = 0L;
        List<String> brewedNames = new ArrayList<>();

        for (Recipe recipe : recipes) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                Ingredient ingredient = lockedIngredients.get(ri.getIngredient().getId());
                ingredient.setCurrentStock(ingredient.getCurrentStock().subtract(ri.getQuantityRequired()));
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
        int newLevel = Barista.levelForXp(barista.getTotalXp());
        barista.setLevel(newLevel);
        baristaRepository.save(barista);

        return new OrderSummaryDTO(brewedNames, orderRevenue, recipes.size(), barista.getTotalXp(), newLevel);
    }
}
