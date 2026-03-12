package com.brewstack.api.controller;

import com.brewstack.api.dto.OrderRequest;
import com.brewstack.api.dto.OrderSummaryDTO;
import com.brewstack.api.service.BrewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/brew")
@RequiredArgsConstructor
public class BrewController {

    private final BrewService brewService;

    /**
     * @deprecated Use POST /api/brew/order instead. This endpoint does not award XP.
     */
    @Deprecated
    @Operation(deprecated = true)
    @PostMapping("/{recipeId}")
    public ResponseEntity<Map<String, String>> brew(@PathVariable Long recipeId) {
        brewService.processBrew(recipeId);
        return ResponseEntity.ok(Map.of("message", "Brew successful! Stock updated. WARNING: This endpoint is deprecated. Use POST /api/brew/order instead."));
    }

    @PostMapping("/order")
    public ResponseEntity<OrderSummaryDTO> order(@Valid @RequestBody OrderRequest request) {
        OrderSummaryDTO summary = brewService.processOrder(request);
        return ResponseEntity.ok(summary);
    }
}
