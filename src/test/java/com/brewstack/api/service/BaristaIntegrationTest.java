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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
}
