package com.brewstack.api.service;

import com.brewstack.api.AbstractIntegrationTest;
import com.brewstack.api.dto.RecipeDTO;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.model.Recipe;
import com.brewstack.api.model.RecipeIngredient;
import com.brewstack.api.repository.IngredientRepository;
import com.brewstack.api.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    private Long ingredientId;
    private Long recipeId;

    @BeforeEach
    void setUp() {
        // Recipes cascade-delete recipe_ingredients; must go first to satisfy FK constraints.
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();

        Ingredient milk = ingredientRepository.save(
                new Ingredient(null, "Whole Milk", new BigDecimal("200.000"), new BigDecimal("20.000"), "ml")
        );
        ingredientId = milk.getId();

        Recipe latte = new Recipe();
        latte.setName("Latte");
        latte.setPrice(new BigDecimal("4.50"));
        latte.setBaseXpReward(10);

        RecipeIngredient ri = new RecipeIngredient();
        ri.setIngredient(milk);
        ri.setQuantityRequired(new BigDecimal("150.000"));
        ri.setRecipe(latte);
        latte.setIngredients(new ArrayList<>(List.of(ri)));

        Recipe saved = recipeRepository.save(latte);
        recipeId = saved.getId();
    }

    // ── GET /api/recipes ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/recipes — returns 200 with at least one recipe")
    void getAllRecipes_returns200WithContent() {
        ResponseEntity<List> response = restTemplate.getForEntity("/api/recipes", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        Map<?, ?> item = (Map<?, ?>) response.getBody().get(0);
        assertThat(item.get("name")).isEqualTo("Latte");
    }

    // ── GET /api/recipes/{id} ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/recipes/{id} — existing id returns 200 with correct DTO")
    void getRecipeById_existingId_returns200() {
        ResponseEntity<RecipeDTO> response = restTemplate.getForEntity(
                "/api/recipes/" + recipeId,
                RecipeDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(recipeId);
        assertThat(response.getBody().name()).isEqualTo("Latte");
        assertThat(response.getBody().ingredients()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/recipes/{id} — unknown id returns 404")
    void getRecipeById_unknownId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/recipes/999999",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── POST /api/recipes ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/recipes — valid request returns 201 with persisted DTO")
    void createRecipe_validRequest_returns201WithDTO() {
        String body = String.format("""
                {
                    "name": "Espresso",
                    "baseXpReward": 5,
                    "price": 2.50,
                    "ingredients": [
                        {"ingredientId": %d, "quantity": 30.000}
                    ]
                }
                """, ingredientId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<RecipeDTO> response = restTemplate.postForEntity(
                "/api/recipes",
                entity,
                RecipeDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Espresso");
        assertThat(response.getBody().ingredients()).hasSize(1);
    }

    // ── PUT /api/recipes/{id} ─────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/recipes/{id} — existing id returns 200 with updated fields")
    void updateRecipe_existingId_returns200WithUpdatedFields() {
        String body = String.format("""
                {
                    "name": "Latte Updated",
                    "baseXpReward": 12,
                    "price": 5.00,
                    "ingredients": [
                        {"ingredientId": %d, "quantity": 180.000}
                    ]
                }
                """, ingredientId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<RecipeDTO> response = restTemplate.exchange(
                "/api/recipes/" + recipeId,
                HttpMethod.PUT,
                entity,
                RecipeDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Latte Updated");
        assertThat(response.getBody().baseXpReward()).isEqualTo(12);
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    // ── DELETE /api/recipes/{id} ──────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/recipes/{id} — existing id returns 204")
    void deleteRecipe_existingId_returns204() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/recipes/" + recipeId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(recipeRepository.findById(recipeId)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/recipes/{id} — unknown id returns 404")
    void deleteRecipe_unknownId_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/recipes/999999",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
