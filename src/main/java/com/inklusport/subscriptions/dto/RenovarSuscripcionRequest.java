package com.inklusport.suscripciones.dto;

import lombok.Data;

@Data
public class RenovarSuscripcionRequest {

    /** Si es null, se renueva con el mismo plan actual. Si viene informado, se registra como CAMBIO_PLAN. */
    private Long planId;
}
