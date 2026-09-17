package com.inklusport.subscriptions.dto;

import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.enums.TipoPagoSuscripcion;
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
    private String organizadorId;
    private TipoPagoSuscripcion tipo;
    private BigDecimal monto;
    private String moneda;
    private String metodoPago;
    private String referenciaTransaccion;
    private EstadoPago estado;
    private LocalDateTime fechaPago;
    private Long comprobanteId;
    private String numeroComprobante;
}
