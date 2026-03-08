package com.brewstack.api.exception;

public class BaristaNotFoundException extends RuntimeException {

    public BaristaNotFoundException(Long id) {
        super("Barista not found with id: " + id);
    }
}
