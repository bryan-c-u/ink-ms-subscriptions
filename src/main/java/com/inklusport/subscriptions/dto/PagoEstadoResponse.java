package com.inklusport.subscriptions.dto;

import com.inklusport.subscriptions.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Estado actual de un cobro, consultado por el frontend al volver del checkout de
 * Mercado Pago (paginas {@code /pagos/exito|pendiente|error}). Permite resolver el
 * pago sin esperar al webhook: si sigue PENDIENTE y se pasa el {@code paymentId} de
 * MP, el backend re-consulta el pago contra la API y aplica la activacion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoEstadoResponse {

    /** Referencia externa generada al crear la preferencia (prefijo {@code PS-} / {@code PE-}). */
    private String referencia;

    /** {@code SUSCRIPCION} o {@code EVENTO}. */
    private String tipo;

    private Long pagoId;

    private EstadoPago estado;

    private BigDecimal monto;

    /** Solo para suscripciones: {@code true} si la suscripcion asociada ya quedo ACTIVA. Null en eventos. */
    private Boolean suscripcionActiva;

    /** {@code true} si este resultado se obtuvo re-consultando Mercado Pago en esta misma llamada. */
    private boolean reconciliadoAhora;
}
