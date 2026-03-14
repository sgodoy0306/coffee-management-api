package com.brewstack.api.controller;

import com.brewstack.api.dto.DailyBalanceDTO;
import com.brewstack.api.service.FinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;

    @GetMapping("/daily-report")
    public ResponseEntity<DailyBalanceDTO> getDailyReport() {
        return ResponseEntity.ok(financialService.getDailyReport());
    }

    @GetMapping("/history")
    public ResponseEntity<Page<DailyBalanceDTO>> getHistory(
            @PageableDefault(size = 30, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(financialService.getHistory(pageable));
    }
}
