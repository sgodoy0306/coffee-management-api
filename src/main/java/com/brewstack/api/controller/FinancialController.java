package com.brewstack.api.controller;

import com.brewstack.api.model.DailyBalance;
import com.brewstack.api.repository.DailyBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinancialController {

    private final DailyBalanceRepository dailyBalanceRepository;

    @GetMapping("/daily-report")
    public ResponseEntity<DailyBalance> getDailyReport() {
        LocalDate today = LocalDate.now();
        DailyBalance balance = dailyBalanceRepository.findById(today)
                .orElse(new DailyBalance(today, BigDecimal.ZERO, 0));
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/history")
    public ResponseEntity<List<DailyBalance>> getHistory() {
        List<DailyBalance> history = dailyBalanceRepository.findAllByOrderByDateDesc();
        return ResponseEntity.ok(history);
    }
}
