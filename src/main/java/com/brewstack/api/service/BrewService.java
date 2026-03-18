package com.brewstack.api.service;

import com.brewstack.api.dto.OrderRequest;
import com.brewstack.api.dto.OrderSummaryDTO;
import com.brewstack.api.exception.BaristaNotFoundException;
import com.brewstack.api.exception.InsufficientStockException;
import com.brewstack.api.exception.RecipeNotFoundException;
import com.brewstack.api.model.Barista;
import com.brewstack.api.model.DailyBalance;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.model.OrderType;
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

    @Transactional
    public OrderSummaryDTO processOrder(OrderRequest request) {
        // baristaId is optional — when null the order is processed without a barista assignment.
        Barista barista = (request.baristaId() != null)
                ? baristaRepository.findById(request.baristaId())
                        .orElseThrow(() -> new BaristaNotFoundException(request.baristaId()))
                : null;

        OrderType orderType = request.orderType() != null ? request.orderType() : OrderType.DINE_IN;

        // Resolve all recipes first so we fail fast on missing IDs.
        // findByIdWithIngredients uses JOIN FETCH to load recipe.ingredients and
        // ri.ingredient in a single query per recipe, eliminating the N+1 lazy
        // SELECT that would otherwise fire when the validation and deduction loops
        // access recipe.getIngredients() below (R44 fix).
        List<Recipe> recipes = new ArrayList<>();
        for (Long recipeId : request.recipeIds()) {
            recipes.add(recipeRepository.findByIdWithIngredients(recipeId)
                    .orElseThrow(() -> new RecipeNotFoundException(recipeId)));
        }

        // Accumulate the total demand per ingredient across all recipes in the order (R35 fix).
        // Without this pre-pass, an order with two recipes sharing an ingredient (e.g. Latte +
        // Cappuccino both requiring Espresso Beans) would pass per-recipe validation even when
        // combined demand exceeds available stock, causing currentStock to go negative.
        Map<Long, BigDecimal> totalDemanded = new HashMap<>();
        for (Recipe recipe : recipes) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                Long ingredientId = ri.getIngredient().getId();
                totalDemanded.merge(ingredientId, ri.getQuantityRequired(), BigDecimal::add);
            }
        }

        // Validate stock for every ingredient against its total demand before touching anything (atomicity).
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
                BigDecimal demanded = totalDemanded.get(ingredientId);
                if (ingredient.getCurrentStock().compareTo(demanded) < 0) {
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

        // Grant XP to barista only when one is assigned to this order.
        long newTotalXp = 0L;
        int newLevel = 0;
        if (barista != null) {
            barista.setTotalXp(barista.getTotalXp() + xpGained);
            newLevel = Barista.levelForXp(barista.getTotalXp());
            barista.setLevel(newLevel);
            baristaRepository.save(barista);
            newTotalXp = barista.getTotalXp();
        }

        log.info("Order processed: baristaId={} recipes={} revenue={} newLevel={} orderType={}",
                request.baristaId(), brewedNames, orderRevenue, newLevel, orderType);

        return new OrderSummaryDTO(brewedNames, orderRevenue, recipes.size(), newTotalXp, newLevel, orderType);
    }
}
