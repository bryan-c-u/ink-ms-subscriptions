package com.inklusport.subscriptions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearSuscripcionRequest {

    @NotNull(message = "El plan es obligatorio")
    private Long planId;

    private Boolean renovacionAutomatica = false;
}
