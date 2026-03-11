package com.brewstack.api.service;

import com.brewstack.api.dto.RestockRequest;
import com.brewstack.api.exception.IngredientNotFoundException;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.repository.IngredientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class StockService {

    private final IngredientRepository ingredientRepository;

    public StockService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public List<Ingredient> getAllStock() {
        log.debug("Fetching all ingredient stock levels");
        return ingredientRepository.findAll();
    }

    @Transactional
    public Ingredient restock(Long id, RestockRequest request) {
        log.info("Restocking ingredient id={} by amount={}", id, request.amount());
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        ingredient.setCurrentStock(ingredient.getCurrentStock() + request.amount());
        Ingredient saved = ingredientRepository.save(ingredient);
        log.info("Ingredient id={} restocked successfully. New stock={}", id, saved.getCurrentStock());
        return saved;
    }
}
