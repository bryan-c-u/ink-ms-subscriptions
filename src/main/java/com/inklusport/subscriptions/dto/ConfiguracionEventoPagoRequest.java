package com.inklusport.subscriptions.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfiguracionEventoPagoRequest {

    @NotBlank(message = "El evento es obligatorio")
    private String eventoId;

    @NotNull(message = "Debe indicar si el evento es pago")
    private Boolean esPago;

    @DecimalMin(value = "0.0", message = "El valor de inscripcion no puede ser negativo")
    private BigDecimal valorInscripcion;
}
