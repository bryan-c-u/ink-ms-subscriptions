package com.inklusport.suscripciones.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlanRequest {

    @NotBlank(message = "El nombre del plan es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private BigDecimal precio;

    @NotNull(message = "El limite de eventos por mes es obligatorio")
    @Min(value = 0, message = "El limite de eventos por mes no puede ser negativo")
    private Integer limiteEventosMes;

    @NotNull(message = "El porcentaje de comision es obligatorio")
    @DecimalMin(value = "0.0", message = "El porcentaje de comision no puede ser negativo")
    private BigDecimal porcentajeComision;

    @NotNull(message = "La duracion en dias es obligatoria")
    @Min(value = 1, message = "La duracion debe ser de al menos 1 dia")
    private Integer duracionDias;

    private List<String> beneficios;
}
