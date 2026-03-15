package com.brewstack.api.service;

import com.brewstack.api.dto.DailyBalanceDTO;
import com.brewstack.api.model.DailyBalance;
import com.brewstack.api.repository.DailyBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
@Service
public class FinancialService {

    private final DailyBalanceRepository dailyBalanceRepository;

    @Transactional(readOnly = true)
    public DailyBalanceDTO getDailyReport() {
        LocalDate today = LocalDate.now();
        log.debug("Fetching daily balance report for date={}", today);
        DailyBalance balance = dailyBalanceRepository.findById(today)
                .orElse(new DailyBalance(today, BigDecimal.ZERO, 0));
        return toDTO(balance);
    }

    @Transactional(readOnly = true)
    public Page<DailyBalanceDTO> getHistory(Pageable pageable) {
        log.debug("Fetching financial history ordered by date descending - page={} size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return dailyBalanceRepository.findAllByOrderByDateDesc(pageable)
                .map(this::toDTO);
    }

    private DailyBalanceDTO toDTO(DailyBalance balance) {
        return new DailyBalanceDTO(
                balance.getDate(),
                balance.getTotalRevenue(),
                balance.getTotalOrders()
        );
    }
}
