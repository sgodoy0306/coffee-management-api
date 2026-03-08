package com.brewstack.api.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String ingredientName) {
        super("Not enough stock for " + ingredientName);
    }
}
