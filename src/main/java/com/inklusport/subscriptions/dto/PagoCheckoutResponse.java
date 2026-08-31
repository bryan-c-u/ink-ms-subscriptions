package com.inklusport.suscripciones.dto;

import com.inklusport.suscripciones.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Respuesta al iniciar un cobro (inscripcion a evento pago o compra de plan):
 * incluye el link de checkout de Mercado Pago para que el cliente complete el pago.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoCheckoutResponse {
    private Long pagoId;
    private BigDecimal monto;
    private EstadoPago estado;
    private String referenciaTransaccion;
    private String checkoutUrl;
}
