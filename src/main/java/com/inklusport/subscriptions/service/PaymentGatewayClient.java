package com.inklusport.suscripciones.service;

import com.inklusport.suscripciones.mercadopago.PaymentPreferenceResult;
import com.inklusport.suscripciones.mercadopago.PaymentStatusResult;

import java.math.BigDecimal;

/**
 * Abstrae la pasarela de pago (RF70) para que el resto del dominio no dependa
 * directamente del SDK de Mercado Pago. Una futura pasarela alternativa solo
 * necesita otra implementacion de esta interfaz.
 */
public interface PaymentGatewayClient {

    PaymentPreferenceResult crearPreferencia(String titulo, BigDecimal monto, String referenciaExterna);

    PaymentStatusResult consultarPago(String paymentIdExterno);
}
