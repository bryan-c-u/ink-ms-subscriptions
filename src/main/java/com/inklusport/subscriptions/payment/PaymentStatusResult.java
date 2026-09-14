package com.inklusport.subscriptions.payment;

import com.inklusport.subscriptions.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResult {
    private String paymentIdExterno;
    private String referenciaExterna;
    private EstadoPago estado;
    private String estadoPasarela;
    private String detalleEstado;
    private BigDecimal montoPagado;
    private String metodoPago;
    private String tipoPago;
    private String payload;
}
