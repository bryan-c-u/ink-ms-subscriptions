package com.inklusport.suscripciones.service;

import com.inklusport.suscripciones.mercadopago.PaymentStatusResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RF70: punto unico de entrada para notificaciones de Mercado Pago. Consulta el pago
 * real contra la API (nunca confia en el payload del webhook) y despacha al servicio
 * correspondiente segun el prefijo de la referencia externa que se genero al crear
 * la preferencia de pago.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagoWebhookService {

    private final PaymentGatewayClient paymentGatewayClient;
    private final PagoSuscripcionService pagoSuscripcionService;
    private final PagoEventoService pagoEventoService;

    public void procesarNotificacion(String paymentIdExterno) {
        PaymentStatusResult status = paymentGatewayClient.consultarPago(paymentIdExterno);
        String referencia = status.getReferenciaExterna();

        if (referencia == null || referencia.isBlank()) {
            log.warn("Notificacion de Mercado Pago {} sin referencia externa, se ignora", paymentIdExterno);
            return;
        }

        if (referencia.startsWith(PagoSuscripcionService.PREFIJO_REFERENCIA)) {
            pagoSuscripcionService.confirmarPago(status);
        } else if (referencia.startsWith(PagoEventoService.PREFIJO_REFERENCIA)) {
            pagoEventoService.confirmarPago(status);
        } else {
            log.warn("Referencia externa con prefijo desconocido: {}", referencia);
        }
    }
}
