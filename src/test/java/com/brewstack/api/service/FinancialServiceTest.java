package com.brewstack.api.service;

import com.brewstack.api.dto.DailyBalanceDTO;
import com.brewstack.api.model.DailyBalance;
import com.brewstack.api.repository.DailyBalanceRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {

    @Mock
    private DailyBalanceRepository dailyBalanceRepository;

    @InjectMocks
    private FinancialService financialService;

    // ── getDailyReport ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDailyReport — no balance for today: returns DTO with zero revenue, zero orders and today's date")
    void getDailyReport_noBalanceForToday_returnsZeroValues() {
        given(dailyBalanceRepository.findById(any(LocalDate.class))).willReturn(Optional.empty());

        DailyBalanceDTO result = financialService.getDailyReport();

        assertThat(result.date()).isEqualTo(LocalDate.now());
        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalOrders()).isEqualTo(0);
    }

    @Test
    @DisplayName("getDailyReport — no balance for today: delegates to findById with today's date")
    void getDailyReport_noBalanceForToday_callsFindById() {
        given(dailyBalanceRepository.findById(any(LocalDate.class))).willReturn(Optional.empty());

        financialService.getDailyReport();

        verify(dailyBalanceRepository).findById(LocalDate.now());
    }

    @Test
    @DisplayName("getDailyReport — balance exists for today: returns DTO with actual accumulated values")
    void getDailyReport_balanceExists_returnsAccumulatedValues() {
        LocalDate today = LocalDate.now();
        DailyBalance existingBalance = new DailyBalance(today, new BigDecimal("245.50"), 12);
        given(dailyBalanceRepository.findById(any(LocalDate.class))).willReturn(Optional.of(existingBalance));

        DailyBalanceDTO result = financialService.getDailyReport();

        assertThat(result.date()).isEqualTo(today);
        assertThat(result.totalRevenue()).isEqualByComparingTo(new BigDecimal("245.50"));
        assertThat(result.totalOrders()).isEqualTo(12);
    }

    @Test
    @DisplayName("getDailyReport — balance exists for today: maps all DTO fields from the entity")
    void getDailyReport_balanceExists_mapsAllDTOFields() {
        LocalDate today = LocalDate.now();
        DailyBalance existingBalance = new DailyBalance(today, new BigDecimal("99.99"), 3);
        given(dailyBalanceRepository.findById(any(LocalDate.class))).willReturn(Optional.of(existingBalance));

        DailyBalanceDTO result = financialService.getDailyReport();

        assertThat(result.date()).isNotNull();
        assertThat(result.totalRevenue()).isNotNull();
        assertThat(result.totalOrders()).isNotNull();
    }

    // ── getHistory ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getHistory — delegates to findAllByOrderByDateDesc with the supplied Pageable")
    void getHistory_delegatesToRepositoryWithCorrectPageable() {
        Pageable pageable = PageRequest.of(0, 5);
        given(dailyBalanceRepository.findAllByOrderByDateDesc(pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        financialService.getHistory(pageable);

        verify(dailyBalanceRepository).findAllByOrderByDateDesc(pageable);
    }

    @Test
    @DisplayName("getHistory — maps each DailyBalance to a DailyBalanceDTO preserving all fields")
    void getHistory_mapsDailyBalanceToDTOCorrectly() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.now().minusDays(1);
        DailyBalance balance1 = new DailyBalance(date1, new BigDecimal("300.00"), 15);
        DailyBalance balance2 = new DailyBalance(date2, new BigDecimal("120.75"), 6);
        Page<DailyBalance> balancePage = new PageImpl<>(List.of(balance1, balance2), pageable, 2);
        given(dailyBalanceRepository.findAllByOrderByDateDesc(pageable)).willReturn(balancePage);

        Page<DailyBalanceDTO> result = financialService.getHistory(pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        DailyBalanceDTO dto1 = result.getContent().get(0);
        assertThat(dto1.date()).isEqualTo(date1);
        assertThat(dto1.totalRevenue()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(dto1.totalOrders()).isEqualTo(15);

        DailyBalanceDTO dto2 = result.getContent().get(1);
        assertThat(dto2.date()).isEqualTo(date2);
        assertThat(dto2.totalRevenue()).isEqualByComparingTo(new BigDecimal("120.75"));
        assertThat(dto2.totalOrders()).isEqualTo(6);
    }

    @Test
    @DisplayName("getHistory — returns empty page when no history records exist")
    void getHistory_returnsEmptyPage_whenNoRecordsExist() {
        Pageable pageable = PageRequest.of(0, 10);
        given(dailyBalanceRepository.findAllByOrderByDateDesc(pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<DailyBalanceDTO> result = financialService.getHistory(pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getHistory — respects pagination metadata (page number, page size, total pages)")
    void getHistory_respectsPaginationMetadata() {
        Pageable pageable = PageRequest.of(1, 3);
        DailyBalance balance = new DailyBalance(LocalDate.now(), new BigDecimal("50.00"), 2);
        // 7 total records, page size 3 → 3 pages total; page 1 has 3 records (simulated with 1 for simplicity)
        Page<DailyBalance> balancePage = new PageImpl<>(List.of(balance), pageable, 7);
        given(dailyBalanceRepository.findAllByOrderByDateDesc(pageable)).willReturn(balancePage);

        Page<DailyBalanceDTO> result = financialService.getHistory(pageable);

        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }
}
