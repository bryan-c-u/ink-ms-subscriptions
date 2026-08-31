package com.inklusport.suscripciones.exception;

public class LimiteEventosExcedidoException extends RuntimeException {
    public LimiteEventosExcedidoException(String organizadorId, int limite) {
        super("El organizador " + organizadorId + " alcanzo el limite de " + limite + " eventos para este mes");
    }
}
