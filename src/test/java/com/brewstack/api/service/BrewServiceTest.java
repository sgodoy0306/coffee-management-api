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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BrewServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private DailyBalanceRepository dailyBalanceRepository;

    @Mock
    private BaristaRepository baristaRepository;

    @InjectMocks
    private BrewService brewService;

    private Barista barista;
    private Ingredient espresso;
    private Ingredient milk;
    private RecipeIngredient ri1;
    private RecipeIngredient ri2;
    private Recipe latte;

    @BeforeEach
    void setUp() {
        barista = new Barista(1L, "Alice", 1, 0L);

        espresso = new Ingredient(1L, "Espresso", new BigDecimal("100.0"), new BigDecimal("10.0"), "ml");
        milk = new Ingredient(2L, "Milk", new BigDecimal("500.0"), new BigDecimal("50.0"), "ml");

        latte = new Recipe();
        latte.setId(10L);
        latte.setName("Latte");
        latte.setPrice(new BigDecimal("4.50"));
        latte.setBaseXpReward(20);

        ri1 = new RecipeIngredient(1L, latte, espresso, new BigDecimal("30.0"));
        ri2 = new RecipeIngredient(2L, latte, milk, new BigDecimal("150.0"));
        latte.setIngredients(List.of(ri1, ri2));
    }

    // ── Happy path ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("processOrder — happy path returns correct summary and awards XP")
    void processOrder_happyPath_returnsCorrectSummary() {
        // Arrange
        OrderRequest request = new OrderRequest(List.of(10L), 1L, null);

        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(recipeRepository.findByIdWithIngredients(10L)).willReturn(Optional.of(latte));
        // Validation loop
        given(ingredientRepository.findByIdWithLock(1L)).willReturn(Optional.of(espresso));
        given(ingredientRepository.findByIdWithLock(2L)).willReturn(Optional.of(milk));
        // Deduction loop reuses the Map built during validation — findByIdWithLock is called once per ingredient (R23)
        given(dailyBalanceRepository.findById(LocalDate.now()))
                .willReturn(Optional.of(new DailyBalance(LocalDate.now(), BigDecimal.ZERO, 0)));
        given(baristaRepository.save(any(Barista.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        OrderSummaryDTO result = brewService.processOrder(request);

        // Assert
        assertThat(result.brewedRecipes()).containsExactly("Latte");
        assertThat(result.totalRevenue()).isEqualByComparingTo("4.50");
        assertThat(result.totalOrders()).isEqualTo(1);
        // XP: baseXpReward = 20 → newLevel = floor(sqrt(20/100)) + 1 = floor(0.447) + 1 = 1
        assertThat(result.baristaXp()).isEqualTo(20L);
        assertThat(result.baristaLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("processOrder — happy path deducts stock for all ingredients")
    void processOrder_happyPath_deductsStock() {
        // Arrange
        OrderRequest request = new OrderRequest(List.of(10L), 1L, null);

        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(recipeRepository.findByIdWithIngredients(10L)).willReturn(Optional.of(latte));
        given(ingredientRepository.findByIdWithLock(1L)).willReturn(Optional.of(espresso));
        given(ingredientRepository.findByIdWithLock(2L)).willReturn(Optional.of(milk));
        given(dailyBalanceRepository.findById(any(LocalDate.class))).willReturn(Optional.empty());
        given(dailyBalanceRepository.save(any(DailyBalance.class))).willAnswer(inv -> inv.getArgument(0));
        given(baristaRepository.save(any(Barista.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        brewService.processOrder(request);

        // Stock values should have been decremented (Hibernate dirty checking handles persistence — no explicit save() call)
        assertThat(espresso.getCurrentStock()).isEqualByComparingTo(new BigDecimal("70.0"));
        assertThat(milk.getCurrentStock()).isEqualByComparingTo(new BigDecimal("350.0"));
    }

    // ── Barista not found ───────────────────────────────────────────────────

    @Test
    @DisplayName("processOrder — throws BaristaNotFoundException when barista is absent")
    void processOrder_unknownBarista_throwsBaristaNotFoundException() {
        // Arrange
        OrderRequest request = new OrderRequest(List.of(10L), 99L, null);
        given(baristaRepository.findById(99L)).willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> brewService.processOrder(request))
                .isInstanceOf(BaristaNotFoundException.class)
                .hasMessageContaining("99");

        // Recipes must never be queried when barista lookup fails
        verify(recipeRepository, never()).findByIdWithIngredients(anyLong());
    }

    // ── Recipe not found ────────────────────────────────────────────────────

    @Test
    @DisplayName("processOrder — throws RecipeNotFoundException when recipe is absent")
    void processOrder_unknownRecipe_throwsRecipeNotFoundException() {
        // Arrange
        OrderRequest request = new OrderRequest(List.of(999L), 1L, null);
        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(recipeRepository.findByIdWithIngredients(999L)).willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> brewService.processOrder(request))
                .isInstanceOf(RecipeNotFoundException.class)
                .hasMessageContaining("999");

        // Stock must never be touched when recipe resolution fails
        verify(ingredientRepository, never()).findByIdWithLock(anyLong());
        verify(ingredientRepository, never()).save(any());
    }

    // ── Insufficient stock ──────────────────────────────────────────────────

    @Test
    @DisplayName("processOrder — throws InsufficientStockException when stock is below required")
    void processOrder_insufficientStock_throwsInsufficientStockException() {
        // Arrange: espresso stock is lower than required (10 < 30)
        espresso.setCurrentStock(new BigDecimal("10.0"));

        OrderRequest request = new OrderRequest(List.of(10L), 1L, null);
        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(recipeRepository.findByIdWithIngredients(10L)).willReturn(Optional.of(latte));
        given(ingredientRepository.findByIdWithLock(1L)).willReturn(Optional.of(espresso));

        // Act & Assert
        assertThatThrownBy(() -> brewService.processOrder(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Espresso");

        // Atomicity invariant: no stock deduction must occur when validation fails
        verify(ingredientRepository, never()).save(any(Ingredient.class));
        verify(dailyBalanceRepository, never()).save(any(DailyBalance.class));
        verify(baristaRepository, never()).save(any(Barista.class));
    }

    @Test
    @DisplayName("processOrder — atomicity: no ingredient is saved if ANY stock check fails")
    void processOrder_insufficientStockOnSecondIngredient_noIngredientSaved() {
        // Arrange: milk stock is lower than required (50 < 150)
        milk.setCurrentStock(new BigDecimal("50.0"));

        OrderRequest request = new OrderRequest(List.of(10L), 1L, null);
        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(recipeRepository.findByIdWithIngredients(10L)).willReturn(Optional.of(latte));
        // First ingredient passes validation
        given(ingredientRepository.findByIdWithLock(1L)).willReturn(Optional.of(espresso));
        // Second ingredient fails
        given(ingredientRepository.findByIdWithLock(2L)).willReturn(Optional.of(milk));

        // Act & Assert
        assertThatThrownBy(() -> brewService.processOrder(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Milk");

        // Neither ingredient must be persisted despite the first one passing
        verify(ingredientRepository, never()).save(any(Ingredient.class));
    }

    // ── R35: shared ingredient across multiple recipes ───────────────────────

    @Test
    @DisplayName("processOrder — R35: throws InsufficientStockException when combined demand for shared ingredient exceeds stock")
    void processOrder_sharedIngredientCombinedDemandExceedsStock_throwsInsufficientStockException() {
        // Arrange: espresso has 25g — each recipe demands 18g — combined demand is 36g > 25g.
        // Per-recipe validation would pass (25 >= 18 twice); total-demand validation must reject it.
        Ingredient sharedEspresso = new Ingredient(1L, "Espresso Beans", new BigDecimal("25.0"), new BigDecimal("5.0"), "g");

        Recipe cappuccino = new Recipe();
        cappuccino.setId(20L);
        cappuccino.setName("Cappuccino");
        cappuccino.setPrice(new BigDecimal("4.00"));
        cappuccino.setBaseXpReward(15);

        RecipeIngredient latteEspresso = new RecipeIngredient(3L, latte, sharedEspresso, new BigDecimal("18.0"));
        RecipeIngredient cappuccinoEspresso = new RecipeIngredient(4L, cappuccino, sharedEspresso, new BigDecimal("18.0"));

        latte.setIngredients(List.of(latteEspresso));
        cappuccino.setIngredients(List.of(cappuccinoEspresso));

        OrderRequest request = new OrderRequest(List.of(10L, 20L), 1L, null);

        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(recipeRepository.findByIdWithIngredients(10L)).willReturn(Optional.of(latte));
        given(recipeRepository.findByIdWithIngredients(20L)).willReturn(Optional.of(cappuccino));
        given(ingredientRepository.findByIdWithLock(1L)).willReturn(Optional.of(sharedEspresso));

        // Act & Assert
        assertThatThrownBy(() -> brewService.processOrder(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Espresso Beans");

        // Atomicity: no stock deduction, no revenue, no XP must be saved
        verify(ingredientRepository, never()).save(any(Ingredient.class));
        verify(dailyBalanceRepository, never()).save(any(DailyBalance.class));
        verify(baristaRepository, never()).save(any(Barista.class));
    }

    @Test
    @DisplayName("processOrder — R35: succeeds when combined demand for shared ingredient is within stock")
    void processOrder_sharedIngredientCombinedDemandWithinStock_deductsTotalFromStock() {
        // Arrange: espresso has 40g — each recipe demands 18g — combined demand is 36g <= 40g → should pass.
        Ingredient sharedEspresso = new Ingredient(1L, "Espresso Beans", new BigDecimal("40.0"), new BigDecimal("5.0"), "g");

        Recipe cappuccino = new Recipe();
        cappuccino.setId(20L);
        cappuccino.setName("Cappuccino");
        cappuccino.setPrice(new BigDecimal("4.00"));
        cappuccino.setBaseXpReward(15);

        RecipeIngredient latteEspresso = new RecipeIngredient(3L, latte, sharedEspresso, new BigDecimal("18.0"));
        RecipeIngredient cappuccinoEspresso = new RecipeIngredient(4L, cappuccino, sharedEspresso, new BigDecimal("18.0"));

        latte.setIngredients(List.of(latteEspresso));
        cappuccino.setIngredients(List.of(cappuccinoEspresso));

        OrderRequest request = new OrderRequest(List.of(10L, 20L), 1L, null);

        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(recipeRepository.findByIdWithIngredients(10L)).willReturn(Optional.of(latte));
        given(recipeRepository.findByIdWithIngredients(20L)).willReturn(Optional.of(cappuccino));
        given(ingredientRepository.findByIdWithLock(1L)).willReturn(Optional.of(sharedEspresso));
        given(dailyBalanceRepository.findById(any(LocalDate.class))).willReturn(Optional.empty());
        given(dailyBalanceRepository.save(any(DailyBalance.class))).willAnswer(inv -> inv.getArgument(0));
        given(baristaRepository.save(any(Barista.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        OrderSummaryDTO result = brewService.processOrder(request);

        // Assert: both recipes brewed, stock deducted twice (18 + 18 = 36, result: 40 - 18 - 18 = 4)
        assertThat(result.brewedRecipes()).containsExactly("Latte", "Cappuccino");
        assertThat(sharedEspresso.getCurrentStock()).isEqualByComparingTo(new BigDecimal("4.0"));
        assertThat(result.totalRevenue()).isEqualByComparingTo("8.50");
        assertThat(result.totalOrders()).isEqualTo(2);
    }
}
