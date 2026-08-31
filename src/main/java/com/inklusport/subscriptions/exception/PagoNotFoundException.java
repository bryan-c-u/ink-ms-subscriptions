package com.inklusport.suscripciones.exception;

public class PagoNotFoundException extends RuntimeException {
    public PagoNotFoundException(String message) {
        super(message);
    }
}
