package com.inklusport.subscriptions.exception;

public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException(Long id) {
        super("No se encontro el plan con id: " + id);
    }
}
