package com.brewstack.api.service;

import com.brewstack.api.exception.RecipeNotFoundException;
import com.brewstack.api.model.Recipe;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private RecipeService recipeService;

    private Recipe latte;

    @BeforeEach
    void setUp() {
        latte = new Recipe();
        latte.setId(10L);
        latte.setName("Latte");
        latte.setPrice(new BigDecimal("4.50"));
        latte.setBaseXpReward(20);
        latte.setIngredients(List.of());
    }

    // ── deleteRecipe ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteRecipe — fetches entity via findByIdWithIngredients then delegates to delete(entity)")
    void deleteRecipe_happyPath_callsDeleteWithEntity() {
        given(recipeRepository.findByIdWithIngredients(10L)).willReturn(Optional.of(latte));

        recipeService.deleteRecipe(10L);

        verify(recipeRepository).findByIdWithIngredients(10L);
        verify(recipeRepository).delete(latte);
    }

    @Test
    @DisplayName("deleteRecipe — throws RecipeNotFoundException when recipe is absent")
    void deleteRecipe_unknownRecipe_throwsNotFoundException() {
        given(recipeRepository.findByIdWithIngredients(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.deleteRecipe(999L))
                .isInstanceOf(RecipeNotFoundException.class)
                .hasMessageContaining("999");

        verify(recipeRepository, never()).delete(any(Recipe.class));
    }
}
