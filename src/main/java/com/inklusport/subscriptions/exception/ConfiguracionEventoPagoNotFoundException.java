package com.inklusport.suscripciones.exception;

public class ConfiguracionEventoPagoNotFoundException extends RuntimeException {
    public ConfiguracionEventoPagoNotFoundException(String eventoId) {
        super("El evento " + eventoId + " no tiene una configuracion de pago registrada");
    }
}
