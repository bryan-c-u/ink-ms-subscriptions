package com.inklusport.suscripciones.dto;

import com.inklusport.suscripciones.enums.EstadoPago;
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
public class PagoSuscripcionResponse {
    private Long id;
    private Long suscripcionId;
    private BigDecimal monto;
    private String metodoPago;
    private String referenciaTransaccion;
    private EstadoPago estado;
    private LocalDateTime fechaPago;
    private Long comprobanteId;
    private String numeroComprobante;
}
