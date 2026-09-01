package com.inklusport.subscriptions.dto;

import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarEstadoSuscripcionRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoSuscripcion estado;
}
