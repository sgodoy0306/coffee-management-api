package com.brewstack.api.service;

import com.brewstack.api.AbstractIntegrationTest;
import com.brewstack.api.dto.IngredientDTO;
import com.brewstack.api.model.Ingredient;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StockIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    private Long ingredientId;

    @BeforeEach
    void setUp() {
        // Recipes must be deleted first: cascade removes recipe_ingredients,
        // which hold the FK reference to ingredients.
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();

        Ingredient ingredient = ingredientRepository.save(
                new Ingredient(null, "Espresso Beans", new BigDecimal("50.000"), new BigDecimal("10.000"), "g")
        );
        ingredientId = ingredient.getId();
    }

    // ── PATCH /api/stock/{id}/restock ─────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/stock/{id}/restock — happy path returns 200 with updated stock")
    void restock_happyPath_returns200WithUpdatedStock() {
        String body = """
                {"amount": 25.000}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<IngredientDTO> response = restTemplate.exchange(
                "/api/stock/" + ingredientId + "/restock",
                HttpMethod.PATCH,
                entity,
                IngredientDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // currentStock was 50.000, added 25.000 → expected 75.000
        assertThat(response.getBody().currentStock()).isEqualByComparingTo(new BigDecimal("75.000"));
    }

    @Test
    @DisplayName("PATCH /api/stock/{id}/restock — unknown ingredient returns 404")
    void restock_unknownIngredient_returns404() {
        String body = """
                {"amount": 10.000}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/stock/999999/restock",
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PATCH /api/stock/{id}/restock — amount=0 returns 400 (Bean Validation)")
    void restock_zeroAmount_returns400() {
        String body = """
                {"amount": 0}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/stock/" + ingredientId + "/restock",
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("PATCH /api/stock/{id}/restock — negative amount returns 400 (Bean Validation)")
    void restock_negativeAmount_returns400() {
        String body = """
                {"amount": -5}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/stock/" + ingredientId + "/restock",
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── GET /api/stock/low ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/stock/low — ingredient below threshold appears in response")
    void getLowStock_returnsIngredientBelowThreshold() {
        // Override the ingredient to have currentStock <= minimumThreshold
        Ingredient lowIngredient = ingredientRepository.findById(ingredientId).orElseThrow();
        lowIngredient.setCurrentStock(new BigDecimal("5.000"));
        lowIngredient.setMinimumThreshold(new BigDecimal("10.000"));
        ingredientRepository.save(lowIngredient);

        ResponseEntity<List> response = restTemplate.getForEntity("/api/stock/low", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        Map<?, ?> item = (Map<?, ?>) response.getBody().get(0);
        assertThat(item.get("name")).isEqualTo("Espresso Beans");
    }

    @Test
    @DisplayName("GET /api/stock/low — returns empty list when all stock is above threshold")
    void getLowStock_returnsEmpty_whenAllStockAboveThreshold() {
        // setUp already creates ingredient with currentStock=50.000 > minimumThreshold=10.000
        ResponseEntity<List> response = restTemplate.getForEntity("/api/stock/low", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    // ── GET /api/stock ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/stock — returns paged response with test ingredient")
    void getAllStock_returnsPagedResponse() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/stock", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isNotNull();
        assertThat(content).hasSize(1);

        Map<?, ?> item = (Map<?, ?>) content.get(0);
        assertThat(item.get("name")).isEqualTo("Espresso Beans");
        assertThat(item.get("unit")).isEqualTo("g");
    }
}
