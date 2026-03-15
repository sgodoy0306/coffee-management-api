package com.brewstack.api.controller;

import com.brewstack.api.dto.OrderRequest;
import com.brewstack.api.dto.OrderSummaryDTO;
import com.brewstack.api.service.BrewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brew")
@RequiredArgsConstructor
public class BrewController {

    private final BrewService brewService;

    @PostMapping("/order")
    public ResponseEntity<OrderSummaryDTO> order(@Valid @RequestBody OrderRequest request) {
        OrderSummaryDTO summary = brewService.processOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }
}
