package com.brewstack.api.controller;

import com.brewstack.api.dto.OrderRequest;
import com.brewstack.api.service.BrewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/brew")
@RequiredArgsConstructor
public class BrewController {

    private final BrewService brewService;

    @PostMapping("/{recipeId}")
    public ResponseEntity<Map<String, String>> brew(@PathVariable Long recipeId) {
        brewService.processBrew(recipeId);
        return ResponseEntity.ok(Map.of("message", "Brew successful! Stock updated."));
    }

    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> order(@Valid @RequestBody OrderRequest request) {
        Map<String, Object> result = brewService.processOrder(request.recipeIds(), request.baristaId());
        return ResponseEntity.ok(result);
    }
}
