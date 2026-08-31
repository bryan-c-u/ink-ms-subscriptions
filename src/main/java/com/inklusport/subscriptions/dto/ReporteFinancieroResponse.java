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
public class ReporteFinancieroResponse {
    private LocalDateTime desde;
    private LocalDateTime hasta;
    private BigDecimal ingresosPorEventos;
    private BigDecimal ingresosPorSuscripciones;
    private long numeroInscritos;
    private List<ReporteEventoItem> detallePorEvento;
}
