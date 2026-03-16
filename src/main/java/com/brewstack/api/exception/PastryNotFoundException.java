package com.brewstack.api.exception;

public class PastryNotFoundException extends RuntimeException {

    public PastryNotFoundException(Long id) {
        super("Pastry not found with id: " + id);
    }
}
