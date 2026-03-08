package com.brewstack.api.controller;

import com.brewstack.api.dto.CreateRecipeRequest;
import com.brewstack.api.dto.RecipeDTO;
import com.brewstack.api.dto.UpdateRecipeRequest;
import com.brewstack.api.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping
    public ResponseEntity<RecipeDTO> createRecipe(@Valid @RequestBody CreateRecipeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeService.createRecipe(request));
    }

    @GetMapping
    public ResponseEntity<List<RecipeDTO>> getAllRecipes() {
        return ResponseEntity.ok(recipeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeDTO> getRecipe(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeDTO> updateRecipe(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateRecipeRequest request) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, request.name(), request.baseXpReward(), request.price()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }
}
