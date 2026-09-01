package com.inklusport.subscriptions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteEventoItem {
    private String eventoId;
    private long numeroInscritos;
    private BigDecimal montoTotal;
    private BigDecimal comisionEstimada;
}
