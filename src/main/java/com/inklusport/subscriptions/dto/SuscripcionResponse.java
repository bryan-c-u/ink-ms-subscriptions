package com.inklusport.suscripciones.dto;

import com.inklusport.suscripciones.enums.EstadoSuscripcion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuscripcionResponse {
    private Long id;
    private String organizadorId;
    private Long planId;
    private String planNombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoSuscripcion estado;
    private Integer eventosCreadosMes;
    private Integer limiteEventosMes;
    private Boolean renovacionAutomatica;
    private LocalDateTime fechaCreacion;
}
