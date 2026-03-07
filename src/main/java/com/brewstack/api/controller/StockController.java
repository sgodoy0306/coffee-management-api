package com.brewstack.api.controller;

import com.brewstack.api.model.Ingredient;
import com.brewstack.api.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
