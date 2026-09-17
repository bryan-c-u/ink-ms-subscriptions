package com.inklusport.subscriptions.dto;

import com.inklusport.subscriptions.enums.EstadoPago;
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
public class PagoEventoResponse {
    private Long id;
    private String usuarioId;
    private String eventoId;
    private String organizadorId;
    /** Snapshot RF66: el historial no depende de un JOIN a sports-ms. */
    private String nombreEvento;
    private BigDecimal monto;
    private String moneda;
    private String metodoPago;
    private String referenciaTransaccion;
    private EstadoPago estado;
    private LocalDateTime fechaPago;
    private Long comprobanteId;
    private String numeroComprobante;
}
