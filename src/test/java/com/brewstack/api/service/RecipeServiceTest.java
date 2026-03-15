package com.brewstack.api.service;

import com.brewstack.api.dto.CreateRecipeRequest;
import com.brewstack.api.dto.RecipeDTO;
import com.brewstack.api.dto.RecipeIngredientRequest;
import com.brewstack.api.dto.UpdateRecipeRequest;
import com.brewstack.api.exception.IngredientNotFoundException;
import com.brewstack.api.exception.RecipeNotFoundException;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.model.Recipe;
import com.brewstack.api.model.RecipeIngredient;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private Ingredient espresso;

    @BeforeEach
    void setUp() {
        latte = new Recipe();
        latte.setId(10L);
        latte.setName("Latte");
        latte.setPrice(new BigDecimal("4.50"));
        latte.setBaseXpReward(20);
        latte.setIngredients(new ArrayList<>(List.of()));

        espresso = new Ingredient();
        espresso.setId(1L);
        espresso.setName("Espresso Beans");
        espresso.setUnit("g");
        espresso.setCurrentStock(new BigDecimal("500.000"));
        espresso.setMinimumThreshold(new BigDecimal("50.000"));
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

    // ── createRecipe ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRecipe — persists recipe and maps ingredient correctly into the returned DTO")
    void createRecipe_happyPath_persistsRecipeWithMappedIngredients() {
        // The service calls save() twice: first to persist the bare Recipe (to get its id),
        // then a second time after attaching the RecipeIngredient list.
        // Both invocations must return an entity that toDTO() can traverse.
        RecipeIngredient link = new RecipeIngredient(null, latte, espresso, new BigDecimal("18.000"));
        Recipe savedWithIngredients = new Recipe();
        savedWithIngredients.setId(10L);
        savedWithIngredients.setName("Latte");
        savedWithIngredients.setPrice(new BigDecimal("4.50"));
        savedWithIngredients.setBaseXpReward(20);
        savedWithIngredients.setImageUrl(null);
        savedWithIngredients.setIngredients(List.of(link));

        // First save returns the bare recipe (no ingredients yet)
        given(recipeRepository.save(any(Recipe.class))).willReturn(latte, savedWithIngredients);
        given(ingredientRepository.findById(1L)).willReturn(Optional.of(espresso));

        CreateRecipeRequest request = new CreateRecipeRequest(
                "Latte",
                20,
                new BigDecimal("4.50"),
                null,
                List.of(new RecipeIngredientRequest(1L, new BigDecimal("18.000")))
        );

        RecipeDTO result = recipeService.createRecipe(request);

        verify(ingredientRepository).findById(1L);
        verify(recipeRepository, org.mockito.Mockito.times(2)).save(any(Recipe.class));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Latte");
        assertThat(result.ingredients()).hasSize(1);
        assertThat(result.ingredients().get(0).ingredientName()).isEqualTo("Espresso Beans");
        assertThat(result.ingredients().get(0).unit()).isEqualTo("g");
        assertThat(result.ingredients().get(0).quantityRequired())
                .isEqualByComparingTo(new BigDecimal("18.000"));
    }

    @Test
    @DisplayName("createRecipe — throws IngredientNotFoundException when ingredientId does not exist")
    void createRecipe_unknownIngredient_throwsIngredientNotFoundException() {
        given(recipeRepository.save(any(Recipe.class))).willReturn(latte);
        given(ingredientRepository.findById(999L)).willReturn(Optional.empty());

        CreateRecipeRequest request = new CreateRecipeRequest(
                "Ghost Recipe",
                10,
                new BigDecimal("3.00"),
                null,
                List.of(new RecipeIngredientRequest(999L, new BigDecimal("5.000")))
        );

        assertThatThrownBy(() -> recipeService.createRecipe(request))
                .isInstanceOf(IngredientNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── updateRecipe ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateRecipe — replaces ingredient list when a non-empty list is provided in the request")
    void updateRecipe_withNewIngredientList_replacesIngredients() {
        // latte starts with one existing ingredient so that clear() has something to remove
        Ingredient milk = new Ingredient();
        milk.setId(2L);
        milk.setName("Whole Milk");
        milk.setUnit("ml");
        milk.setCurrentStock(new BigDecimal("1000.000"));
        milk.setMinimumThreshold(new BigDecimal("100.000"));

        RecipeIngredient existingLink = new RecipeIngredient(5L, latte, milk, new BigDecimal("150.000"));
        latte.setIngredients(new ArrayList<>(List.of(existingLink)));

        // After save, the returned recipe contains the new ingredient
        RecipeIngredient newLink = new RecipeIngredient(null, latte, espresso, new BigDecimal("18.000"));
        Recipe updatedRecipe = new Recipe();
        updatedRecipe.setId(10L);
        updatedRecipe.setName("Latte Revisado");
        updatedRecipe.setPrice(new BigDecimal("5.00"));
        updatedRecipe.setBaseXpReward(25);
        updatedRecipe.setImageUrl(null);
        updatedRecipe.setIngredients(List.of(newLink));

        given(recipeRepository.findByIdWithIngredients(10L)).willReturn(Optional.of(latte));
        given(ingredientRepository.findById(1L)).willReturn(Optional.of(espresso));
        given(recipeRepository.save(latte)).willReturn(updatedRecipe);

        UpdateRecipeRequest request = new UpdateRecipeRequest(
                "Latte Revisado",
                25,
                new BigDecimal("5.00"),
                null,
                List.of(new RecipeIngredientRequest(1L, new BigDecimal("18.000")))
        );

        RecipeDTO result = recipeService.updateRecipe(10L, request);

        verify(recipeRepository).findByIdWithIngredients(10L);
        verify(ingredientRepository).findById(1L);
        verify(recipeRepository).save(latte);

        assertThat(result.name()).isEqualTo("Latte Revisado");
        assertThat(result.ingredients()).hasSize(1);
        assertThat(result.ingredients().get(0).ingredientName()).isEqualTo("Espresso Beans");
    }

    @Test
    @DisplayName("updateRecipe — preserves existing ingredients when the request ingredients list is null")
    void updateRecipe_withNullIngredientList_preservesExistingIngredients() {
        RecipeIngredient existingLink = new RecipeIngredient(5L, latte, espresso, new BigDecimal("18.000"));
        latte.setIngredients(new ArrayList<>(List.of(existingLink)));

        Recipe savedRecipe = new Recipe();
        savedRecipe.setId(10L);
        savedRecipe.setName("Latte Pro");
        savedRecipe.setPrice(new BigDecimal("5.00"));
        savedRecipe.setBaseXpReward(30);
        savedRecipe.setImageUrl(null);
        savedRecipe.setIngredients(List.of(existingLink));

        given(recipeRepository.findByIdWithIngredients(10L)).willReturn(Optional.of(latte));
        given(recipeRepository.save(latte)).willReturn(savedRecipe);

        // ingredients == null → the service's if-block is skipped entirely
        UpdateRecipeRequest request = new UpdateRecipeRequest(
                "Latte Pro",
                30,
                new BigDecimal("5.00"),
                null,
                null
        );

        RecipeDTO result = recipeService.updateRecipe(10L, request);

        // ingredientRepository must never be consulted when ingredients is null
        verify(ingredientRepository, never()).findById(any());
        verify(recipeRepository).save(latte);

        assertThat(result.ingredients()).hasSize(1);
        assertThat(result.ingredients().get(0).ingredientName()).isEqualTo("Espresso Beans");
    }

    @Test
    @DisplayName("updateRecipe — throws RecipeNotFoundException when recipe id does not exist")
    void updateRecipe_unknownRecipe_throwsRecipeNotFoundException() {
        given(recipeRepository.findByIdWithIngredients(999L)).willReturn(Optional.empty());

        UpdateRecipeRequest request = new UpdateRecipeRequest(
                "Phantom",
                0,
                new BigDecimal("1.00"),
                null,
                null
        );

        assertThatThrownBy(() -> recipeService.updateRecipe(999L, request))
                .isInstanceOf(RecipeNotFoundException.class)
                .hasMessageContaining("999");

        verify(recipeRepository, never()).save(any(Recipe.class));
    }
}
