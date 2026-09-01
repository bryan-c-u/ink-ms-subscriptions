package com.inklusport.subscriptions.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlanRequest {

    @NotBlank
    private String nombre;
    private String descripcion;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal precio;

    private String moneda = "COP";

    @Min(0)
    private Integer limiteEventosMes;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal porcentajeComision;

    @NotNull
    @Min(1)
    private Integer duracionDias;

    private Boolean esGratuito;
    private Boolean esPlanInicial;
    private List<String> beneficios;
}
