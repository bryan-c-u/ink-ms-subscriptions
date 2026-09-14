package com.inklusport.subscriptions.mercadopago;

import com.inklusport.subscriptions.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Estado real de un pago consultado contra la pasarela (fuente de verdad, no el payload del webhook). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResult {
    private String paymentIdExterno;
    private String referenciaExterna;
    private EstadoPago estado;
    private BigDecimal montoPagado;
    private String metodoPago;
}
