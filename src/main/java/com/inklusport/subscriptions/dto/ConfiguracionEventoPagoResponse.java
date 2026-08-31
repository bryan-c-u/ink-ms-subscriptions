package com.inklusport.suscripciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionEventoPagoResponse {
    private Long id;
    private String eventoId;
    private String organizadorId;
    private Boolean esPago;
    private BigDecimal valorInscripcion;
    private BigDecimal porcentajeComision;
    private LocalDateTime fechaCreacion;
}
