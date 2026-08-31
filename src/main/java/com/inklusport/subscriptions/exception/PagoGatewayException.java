package com.inklusport.suscripciones.exception;

public class PagoGatewayException extends RuntimeException {
    public PagoGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    public PagoGatewayException(String message) {
        super(message);
    }
}
