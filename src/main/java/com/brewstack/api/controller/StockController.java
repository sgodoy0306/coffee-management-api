package com.brewstack.api.controller;

import com.brewstack.api.dto.RestockRequest;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<Ingredient>> getAllStock() {
        return ResponseEntity.ok(stockService.getAllStock());
    }

    @PatchMapping("/{id}/restock")
    public ResponseEntity<Ingredient> restock(@PathVariable Long id,
                                               @Valid @RequestBody RestockRequest request) {
        return ResponseEntity.ok(stockService.restock(id, request));
    }
}
