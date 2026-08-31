package com.inklusport.suscripciones.exception;

public class PlanInactivoException extends RuntimeException {
    public PlanInactivoException(Long id) {
        super("El plan con id " + id + " no esta activo");
    }
}
