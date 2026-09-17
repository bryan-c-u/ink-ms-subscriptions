package com.inklusport.subscriptions.dto;

import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambiarEstadoSuscripcionRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoSuscripcion estado;

    /** Motivo opcional; queda en historial_suscripcion.notas. */
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String motivo;
}
