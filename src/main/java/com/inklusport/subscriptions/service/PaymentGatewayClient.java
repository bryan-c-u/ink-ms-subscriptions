package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.mercadopago.PaymentPreferenceResult;
import com.inklusport.subscriptions.mercadopago.PaymentStatusResult;

import java.math.BigDecimal;

public interface PaymentGatewayClient {

    PaymentPreferenceResult crearPreferencia(String titulo, BigDecimal monto, String referenciaExterna);

    PaymentStatusResult consultarPago(String paymentIdExterno);

    /**
     * Cobra directamente con un token de tarjeta generado en el navegador (checkout propio,
     * sin redirigir a la interfaz de Mercado Pago). El token nunca expone el numero de
     * tarjeta: lo genero el SDK JS de MP en el cliente.
     */
    PaymentStatusResult procesarPago(String cardToken, BigDecimal monto, String referenciaExterna,
                                      String descripcion, Integer cuotas, String paymentMethodId,
                                      String payerEmail, String docType, String docNumber);
}
