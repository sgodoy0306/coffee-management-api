package com.brewstack.api.config;

import com.brewstack.api.model.Ingredient;
import com.brewstack.api.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final IngredientRepository ingredientRepository;

    @Override
    public void run(String... args) {
        if (ingredientRepository.count() > 0) {
            return;
        }

        ingredientRepository.saveAll(List.of(
                new Ingredient(null, "Espresso Beans", 5000.0, 500.0, "grams"),
                new Ingredient(null, "Whole Milk", 10000.0, 1000.0, "ml")
        ));
    }
}
