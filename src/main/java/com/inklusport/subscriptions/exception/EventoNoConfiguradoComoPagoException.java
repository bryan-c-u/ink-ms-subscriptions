package com.inklusport.suscripciones.exception;

public class EventoNoConfiguradoComoPagoException extends RuntimeException {
    public EventoNoConfiguradoComoPagoException(String eventoId) {
        super("El evento " + eventoId + " no esta configurado como evento de pago");
    }
}
