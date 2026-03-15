package com.brewstack.api.service;

import com.brewstack.api.AbstractIntegrationTest;
import com.brewstack.api.dto.DailyBalanceDTO;
import com.brewstack.api.model.DailyBalance;
import com.brewstack.api.repository.DailyBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DailyBalanceRepository dailyBalanceRepository;

    @BeforeEach
    void setUp() {
        dailyBalanceRepository.deleteAll();
    }

    // ── GET /api/finance/daily-report ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/finance/daily-report — no balance today returns 200 with zero values")
    void getDailyReport_noBalanceToday_returns200WithZeroValues() {
        ResponseEntity<DailyBalanceDTO> response = restTemplate.getForEntity(
                "/api/finance/daily-report",
                DailyBalanceDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().date()).isEqualTo(LocalDate.now());
        assertThat(response.getBody().totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getBody().totalOrders()).isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/finance/daily-report — with balance for today returns 200 with accumulated values")
    void getDailyReport_withBalance_returns200WithAccumulatedValues() {
        dailyBalanceRepository.save(
                new DailyBalance(LocalDate.now(), new BigDecimal("12.50"), 3)
        );

        ResponseEntity<DailyBalanceDTO> response = restTemplate.getForEntity(
                "/api/finance/daily-report",
                DailyBalanceDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().date()).isEqualTo(LocalDate.now());
        assertThat(response.getBody().totalRevenue()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(response.getBody().totalOrders()).isEqualTo(3);
    }

    // ── GET /api/finance/history ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/finance/history — returns 200 with paged content")
    void getHistory_returns200WithPagedContent() {
        dailyBalanceRepository.save(new DailyBalance(LocalDate.now().minusDays(1), new BigDecimal("8.00"), 2));
        dailyBalanceRepository.save(new DailyBalance(LocalDate.now().minusDays(2), new BigDecimal("15.00"), 4));

        ResponseEntity<Map> response = restTemplate.getForEntity("/api/finance/history", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isNotNull();
        assertThat(content).hasSize(2);

        // Ordered by date DESC: most recent day first
        Map<?, ?> first = (Map<?, ?>) content.get(0);
        assertThat(first.get("totalOrders")).isEqualTo(2);
    }
}
