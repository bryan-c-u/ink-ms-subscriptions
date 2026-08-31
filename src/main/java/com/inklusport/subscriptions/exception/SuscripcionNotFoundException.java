package com.inklusport.suscripciones.exception;

public class SuscripcionNotFoundException extends RuntimeException {
    public SuscripcionNotFoundException(Long id) {
        super("No se encontro la suscripcion con id: " + id);
    }

    public SuscripcionNotFoundException(String message) {
        super(message);
    }
}
