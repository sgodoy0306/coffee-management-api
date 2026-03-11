package com.brewstack.api.controller;

import com.brewstack.api.model.DailyBalance;
import com.brewstack.api.service.FinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;

    @GetMapping("/daily-report")
    public ResponseEntity<DailyBalance> getDailyReport() {
        return ResponseEntity.ok(financialService.getDailyReport());
    }

    @GetMapping("/history")
    public ResponseEntity<List<DailyBalance>> getHistory() {
        return ResponseEntity.ok(financialService.getHistory());
    }
}
