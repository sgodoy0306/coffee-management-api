package com.brewstack.api.service;

import com.brewstack.api.AbstractIntegrationTest;
import com.brewstack.api.dto.LevelUpDTO;
import com.brewstack.api.model.Barista;
import com.brewstack.api.repository.BaristaRepository;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaristaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BaristaRepository baristaRepository;

    @BeforeEach
    void setUp() {
        baristaRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/baristas/{id}/practice — awards XP and returns LevelUpDTO")
    void practice_endpoint_awardsXpAndReturnsLevelUpDTO() {
        Barista saved = baristaRepository.save(new Barista(null, "Bob", 1, 0L));

        String body = """
                {"rating": 2}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<LevelUpDTO> response = restTemplate.postForEntity(
                "/api/baristas/" + saved.getId() + "/practice",
                entity,
                LevelUpDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // rating=2 → xpGained=100 → totalXp=100 → level=2
        assertThat(response.getBody().totalXp()).isEqualTo(100L);
        assertThat(response.getBody().newLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST /api/baristas/{id}/practice — returns 404 for unknown barista")
    void practice_endpoint_returns404ForUnknownBarista() {
        String body = """
                {"rating": 5}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/baristas/999999/practice",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── ErrorResponse shape validation ───────────────────────────────────────

    @Test
    @DisplayName("POST /api/baristas/{id}/practice — 404 body has correct ErrorResponse shape")
    void practice_unknownBarista_errorResponseHasCorrectShape() {
        String body = """
                {"rating": 5}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/baristas/999999/practice",
                HttpMethod.POST,
                entity,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Map<String, Object> errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody).containsKeys("timestamp", "status", "error", "message", "path");
        assertThat(errorBody.get("status")).isEqualTo(404);
        assertThat(errorBody.get("path").toString()).contains("/api/baristas");
        // Detects R53 regression: LocalDateTime must serialize as ISO-8601 String,
        // not as a JSON array [2026,3,15,...] when WRITE_DATES_AS_TIMESTAMPS=false
        assertThat(errorBody.get("timestamp")).isInstanceOf(String.class);
    }

    // ── Bean Validation — POST /api/baristas ─────────────────────────────────

    @Test
    @DisplayName("POST /api/baristas — blank name returns 400")
    void createBarista_blankName_returns400() {
        String body = """
                {"name": ""}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/baristas",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/baristas — blank name error body has correct ErrorResponse shape")
    void createBarista_blankName_errorResponseHasCorrectShape() {
        String body = """
                {"name": ""}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/baristas",
                HttpMethod.POST,
                entity,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody).containsKeys("timestamp", "status", "error", "message", "path");
        assertThat(errorBody.get("status")).isEqualTo(400);
        assertThat(errorBody.get("path").toString()).contains("/api/baristas");
        assertThat(errorBody.get("timestamp")).isInstanceOf(String.class);
    }

    // ── Bean Validation — POST /api/baristas/{id}/practice ───────────────────

    @Test
    @DisplayName("POST /api/baristas/{id}/practice — rating below minimum (0) returns 400")
    void practice_ratingBelowMin_returns400() {
        Barista saved = baristaRepository.save(new Barista(null, "Alice", 1, 0L));

        String body = """
                {"rating": 0}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/baristas/" + saved.getId() + "/practice",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/baristas/{id}/practice — rating above maximum (11) returns 400")
    void practice_ratingAboveMax_returns400() {
        Barista saved = baristaRepository.save(new Barista(null, "Alice", 1, 0L));

        String body = """
                {"rating": 11}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/baristas/" + saved.getId() + "/practice",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
