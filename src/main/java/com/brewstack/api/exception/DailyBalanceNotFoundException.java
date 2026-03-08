package com.brewstack.api.exception;

import java.time.LocalDate;

public class DailyBalanceNotFoundException extends RuntimeException {

    public DailyBalanceNotFoundException(LocalDate date) {
        super("No balance record found for date: " + date);
    }
}
