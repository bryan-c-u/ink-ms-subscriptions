package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.payment.PaymentPreferenceResult;
import com.inklusport.subscriptions.payment.PaymentStatusResult;

import java.math.BigDecimal;

public interface PaymentGatewayClient {

    PaymentPreferenceResult crearPreferencia(String titulo, BigDecimal monto, String referenciaExterna);

    PaymentStatusResult consultarPago(String paymentIdExterno);
}
