package com.brewstack.api.service;

import com.brewstack.api.dto.DailyBalanceDTO;
import com.brewstack.api.model.DailyBalance;
import com.brewstack.api.repository.DailyBalanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class FinancialService {

    private final DailyBalanceRepository dailyBalanceRepository;

    public FinancialService(DailyBalanceRepository dailyBalanceRepository) {
        this.dailyBalanceRepository = dailyBalanceRepository;
    }

    public DailyBalanceDTO getDailyReport() {
        LocalDate today = LocalDate.now();
        log.debug("Fetching daily balance report for date={}", today);
        DailyBalance balance = dailyBalanceRepository.findById(today)
                .orElse(new DailyBalance(today, BigDecimal.ZERO, 0));
        return toDTO(balance);
    }

    public List<DailyBalanceDTO> getHistory() {
        log.debug("Fetching full financial history ordered by date descending");
        return dailyBalanceRepository.findAllByOrderByDateDesc().stream()
                .map(this::toDTO)
                .toList();
    }

    private DailyBalanceDTO toDTO(DailyBalance balance) {
        return new DailyBalanceDTO(
                balance.getDate(),
                balance.getTotalRevenue(),
                balance.getTotalOrders()
        );
    }
}
