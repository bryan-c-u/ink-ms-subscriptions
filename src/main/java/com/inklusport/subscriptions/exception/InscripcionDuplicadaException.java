package com.inklusport.subscriptions.exception;

public class InscripcionDuplicadaException extends RuntimeException {
    public InscripcionDuplicadaException(String usuarioId, String eventoId) {
        super("El usuario " + usuarioId + " ya tiene una inscripcion pagada para el evento " + eventoId);
    }
}
