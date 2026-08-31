package com.inklusport.suscripciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer limiteEventosMes;
    private BigDecimal porcentajeComision;
    private Integer duracionDias;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private List<String> beneficios;
}
