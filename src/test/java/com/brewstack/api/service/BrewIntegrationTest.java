package com.brewstack.api.service;

import com.brewstack.api.AbstractIntegrationTest;
import com.brewstack.api.dto.OrderSummaryDTO;
import com.brewstack.api.model.Barista;
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

class BrewIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BaristaRepository baristaRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private DailyBalanceRepository dailyBalanceRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    private Long baristaId;
    private Long recipeId;

    @BeforeEach
    void setUp() {
        dailyBalanceRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        baristaRepository.deleteAll();

        Barista barista = baristaRepository.save(new Barista(null, "Carlos", 1, 0L));
        baristaId = barista.getId();

        Ingredient espresso = ingredientRepository.save(
                new Ingredient(null, "Espresso", new BigDecimal("500.0"), new BigDecimal("10.0"), "ml"));

        Recipe cappuccino = new Recipe();
        cappuccino.setName("Cappuccino");
        cappuccino.setPrice(new BigDecimal("3.75"));
        cappuccino.setBaseXpReward(15);

        RecipeIngredient ri = new RecipeIngredient();
        ri.setIngredient(espresso);
        ri.setQuantityRequired(new BigDecimal("30.0"));
        ri.setRecipe(cappuccino);
        cappuccino.setIngredients(List.of(ri));

        Recipe saved = recipeRepository.save(cappuccino);
        recipeId = saved.getId();
    }

    @Test
    @DisplayName("POST /api/brew/order — happy path returns 201 with correct summary")
    void order_endpoint_happyPath_returns201WithSummary() {
        String body = String.format("""
                {
                    "recipeIds": [%d],
                    "baristaId": %d
                }
                """, recipeId, baristaId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<OrderSummaryDTO> response = restTemplate.postForEntity(
                "/api/brew/order",
                entity,
                OrderSummaryDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().brewedRecipes()).containsExactly("Cappuccino");
        assertThat(response.getBody().totalRevenue()).isEqualByComparingTo("3.75");
        assertThat(response.getBody().totalOrders()).isEqualTo(1);
        assertThat(response.getBody().baristaXp()).isEqualTo(15L);
        assertThat(response.getBody().baristaLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/brew/order — insufficient stock returns 409")
    void order_endpoint_insufficientStock_returns409() {
        Ingredient depleted = ingredientRepository.findByName("Espresso").orElseThrow();
        depleted.setCurrentStock(new BigDecimal("5.0"));
        ingredientRepository.save(depleted);

        String body = String.format("""
                {
                    "recipeIds": [%d],
                    "baristaId": %d
                }
                """, recipeId, baristaId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/brew/order",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("POST /api/brew/order — unknown recipe returns 404")
    void order_endpoint_unknownRecipe_returns404() {
        String body = String.format("""
                {
                    "recipeIds": [999999],
                    "baristaId": %d
                }
                """, baristaId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/brew/order",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── ErrorResponse shape validation ───────────────────────────────────────

    @Test
    @DisplayName("POST /api/brew/order — unknown barista 404 body has correct ErrorResponse shape")
    void order_unknownBarista_errorResponseHasCorrectShape() {
        String body = String.format("""
                {
                    "recipeIds": [%d],
                    "baristaId": 999999
                }
                """, recipeId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/brew/order",
                HttpMethod.POST,
                entity,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Map<String, Object> errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody).containsKeys("timestamp", "status", "error", "message", "path");
        assertThat(errorBody.get("status")).isEqualTo(404);
        assertThat(errorBody.get("path").toString()).contains("/api/brew/order");
        // Detects R53 regression: LocalDateTime must serialize as ISO-8601 String,
        // not as a JSON array [2026,3,15,...] when WRITE_DATES_AS_TIMESTAMPS=false
        assertThat(errorBody.get("timestamp")).isInstanceOf(String.class);
    }

    // ── Bean Validation — POST /api/brew/order ───────────────────────────────

    @Test
    @DisplayName("POST /api/brew/order — empty recipeIds list returns 400")
    void order_emptyRecipeList_returns400() {
        String body = String.format("""
                {
                    "recipeIds": [],
                    "baristaId": %d
                }
                """, baristaId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/brew/order",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
