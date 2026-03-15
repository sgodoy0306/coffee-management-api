package com.brewstack.api.controller;

import com.brewstack.api.dto.IngredientDTO;
import com.brewstack.api.dto.RestockRequest;
import com.brewstack.api.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<Page<IngredientDTO>> getAllStock(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(stockService.getAllStock(pageable));
    }

    @GetMapping("/low")
    public ResponseEntity<List<IngredientDTO>> getLowStockIngredients() {
        return ResponseEntity.ok(stockService.getLowStockIngredients());
    }

    @PatchMapping("/{id}/restock")
    public ResponseEntity<IngredientDTO> restock(@PathVariable Long id,
                                                 @Valid @RequestBody RestockRequest request) {
        return ResponseEntity.ok(stockService.restock(id, request));
    }
}
