package com.brewstack.api.controller;

import com.brewstack.api.dto.RestockRequest;
import com.brewstack.api.exception.IngredientNotFoundException;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.repository.IngredientRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final IngredientRepository ingredientRepository;

    @GetMapping
    public ResponseEntity<List<Ingredient>> getAllStock() {
        return ResponseEntity.ok(ingredientRepository.findAll());
    }

    @PatchMapping("/{id}/restock")
    public ResponseEntity<Ingredient> restock(@PathVariable Long id,
                                               @Valid @RequestBody RestockRequest request) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        ingredient.setCurrentStock(ingredient.getCurrentStock() + request.amount());
        return ResponseEntity.ok(ingredientRepository.save(ingredient));
    }
}
