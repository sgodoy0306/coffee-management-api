package com.brewstack.api.service;

import com.brewstack.api.dto.IngredientDTO;
import com.brewstack.api.dto.RestockRequest;
import com.brewstack.api.exception.IngredientNotFoundException;
import com.brewstack.api.model.Ingredient;
import com.brewstack.api.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class StockService {

    private final IngredientRepository ingredientRepository;

    @Transactional(readOnly = true)
    public Page<IngredientDTO> getAllStock(Pageable pageable) {
        log.debug("Fetching ingredient stock levels - page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return ingredientRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<IngredientDTO> getLowStockIngredients() {
        log.debug("Fetching low-stock ingredients (currentStock <= minimumThreshold)");
        List<IngredientDTO> result = ingredientRepository.findLowStockIngredients()
                .stream()
                .map(this::toDTO)
                .toList();
        log.debug("Found {} low-stock ingredient(s)", result.size());
        return result;
    }

    @Transactional
    public IngredientDTO restock(Long id, RestockRequest request) {
        log.info("Restocking ingredient id={} by amount={}", id, request.amount());
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        ingredient.setCurrentStock(ingredient.getCurrentStock().add(request.amount()));
        Ingredient saved = ingredientRepository.save(ingredient);
        log.info("Ingredient id={} restocked successfully. New stock={}", id, saved.getCurrentStock());
        return toDTO(saved);
    }

    private IngredientDTO toDTO(Ingredient ingredient) {
        return new IngredientDTO(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getCurrentStock(),
                ingredient.getMinimumThreshold(),
                ingredient.getUnit()
        );
    }
}
