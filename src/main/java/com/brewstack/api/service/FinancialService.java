package com.brewstack.api.service;

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

    public DailyBalance getDailyReport() {
        LocalDate today = LocalDate.now();
        log.debug("Fetching daily balance report for date={}", today);
        return dailyBalanceRepository.findById(today)
                .orElse(new DailyBalance(today, BigDecimal.ZERO, 0));
    }

    public List<DailyBalance> getHistory() {
        log.debug("Fetching full financial history ordered by date descending");
        return dailyBalanceRepository.findAllByOrderByDateDesc();
    }
}
