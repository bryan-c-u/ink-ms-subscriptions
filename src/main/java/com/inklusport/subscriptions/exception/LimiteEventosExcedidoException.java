package com.inklusport.subscriptions.exception;

public class LimiteEventosExcedidoException extends RuntimeException {
    public LimiteEventosExcedidoException(String organizadorId, Integer limite) {
        super("El organizador " + organizadorId + " alcanzo el limite de " + limite + " eventos para este mes");
    }
}
