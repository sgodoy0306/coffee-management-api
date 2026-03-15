package com.brewstack.api.service;

import com.brewstack.api.dto.IngredientDTO;
import com.brewstack.api.dto.RestockRequest;
import com.brewstack.api.exception.IngredientNotFoundException;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.repository.IngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private StockService stockService;

    private Ingredient espressoBeans;

    @BeforeEach
    void setUp() {
        espressoBeans = new Ingredient(
                1L,
                "Espresso Beans",
                new BigDecimal("18.000"),
                new BigDecimal("5.000"),
                "g"
        );
    }

    // ── restock ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("restock — happy path: adds restock amount to current stock and returns updated DTO")
    void restock_happyPath_addsAmountToCurrentStock() {
        RestockRequest request = new RestockRequest(new BigDecimal("10.000"));
        Ingredient saved = new Ingredient(
                1L,
                "Espresso Beans",
                new BigDecimal("28.000"),
                new BigDecimal("5.000"),
                "g"
        );
        given(ingredientRepository.findById(1L)).willReturn(Optional.of(espressoBeans));
        given(ingredientRepository.save(espressoBeans)).willReturn(saved);

        IngredientDTO result = stockService.restock(1L, request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Espresso Beans");
        assertThat(result.currentStock()).isEqualByComparingTo(new BigDecimal("28.000"));
        assertThat(result.minimumThreshold()).isEqualByComparingTo(new BigDecimal("5.000"));
        assertThat(result.unit()).isEqualTo("g");
    }

    @Test
    @DisplayName("restock — happy path: calls save() with the updated ingredient instance")
    void restock_happyPath_callsSaveWithUpdatedIngredient() {
        RestockRequest request = new RestockRequest(new BigDecimal("10.000"));
        given(ingredientRepository.findById(1L)).willReturn(Optional.of(espressoBeans));
        given(ingredientRepository.save(any(Ingredient.class))).willAnswer(inv -> inv.getArgument(0));

        stockService.restock(1L, request);

        verify(ingredientRepository).save(espressoBeans);
    }

    @Test
    @DisplayName("restock — unknown ingredient: throws IngredientNotFoundException")
    void restock_unknownIngredient_throwsIngredientNotFoundException() {
        RestockRequest request = new RestockRequest(new BigDecimal("10.000"));
        given(ingredientRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.restock(99L, request))
                .isInstanceOf(IngredientNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("restock — unknown ingredient: never calls save()")
    void restock_unknownIngredient_neverCallsSave() {
        RestockRequest request = new RestockRequest(new BigDecimal("10.000"));
        given(ingredientRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.restock(99L, request))
                .isInstanceOf(IngredientNotFoundException.class);

        verify(ingredientRepository, never()).save(any(Ingredient.class));
    }

    // ── getLowStockIngredients ────────────────────────────────────────────────

    @Test
    @DisplayName("getLowStockIngredients — returns list of ingredients below threshold mapped to DTOs")
    void getLowStockIngredients_returnsIngredientsBelowThreshold() {
        Ingredient lowMilk = new Ingredient(2L, "Milk", new BigDecimal("3.000"), new BigDecimal("5.000"), "ml");
        given(ingredientRepository.findLowStockIngredients()).willReturn(List.of(lowMilk));

        List<IngredientDTO> result = stockService.getLowStockIngredients();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(2L);
        assertThat(result.get(0).name()).isEqualTo("Milk");
        assertThat(result.get(0).currentStock()).isEqualByComparingTo(new BigDecimal("3.000"));
        assertThat(result.get(0).minimumThreshold()).isEqualByComparingTo(new BigDecimal("5.000"));
        assertThat(result.get(0).unit()).isEqualTo("ml");
    }

    @Test
    @DisplayName("getLowStockIngredients — returns empty list when no ingredients are below threshold")
    void getLowStockIngredients_returnsEmptyList_whenNoLowStock() {
        given(ingredientRepository.findLowStockIngredients()).willReturn(List.of());

        List<IngredientDTO> result = stockService.getLowStockIngredients();

        assertThat(result).isEmpty();
    }

    // ── getAllStock (paginated) ────────────────────────────────────────────────

    @Test
    @DisplayName("getAllStock — delegates to findAll(Pageable) and maps results to DTOs")
    void getAllStock_returnsPagedIngredients() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ingredient> ingredientPage = new PageImpl<>(List.of(espressoBeans), pageable, 1);
        given(ingredientRepository.findAll(pageable)).willReturn(ingredientPage);

        Page<IngredientDTO> result = stockService.getAllStock(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);

        IngredientDTO dto = result.getContent().get(0);
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Espresso Beans");
        assertThat(dto.currentStock()).isEqualByComparingTo(new BigDecimal("18.000"));
        assertThat(dto.minimumThreshold()).isEqualByComparingTo(new BigDecimal("5.000"));
        assertThat(dto.unit()).isEqualTo("g");

        verify(ingredientRepository).findAll(pageable);
    }
}
