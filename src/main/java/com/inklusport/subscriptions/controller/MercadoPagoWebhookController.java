package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.mercadopago.MercadoPagoSignatureVerifier;
import com.inklusport.subscriptions.mercadopago.MercadoPagoWebhookPayload;
import com.inklusport.subscriptions.service.PagoWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RF70: recibe las notificaciones (IPN/webhook) de Mercado Pago. Acepta tanto el
 * formato con body JSON como el formato clasico por query params, ya que Mercado
 * Pago ha usado ambos segun la integracion. Siempre responde 200 para evitar que
 * Mercado Pago reintente indefinidamente; los errores solo se registran en el log.
 *
 * Antes de procesar valida la firma HMAC ({@code x-signature} / {@code x-request-id});
 * si {@code mercadopago.webhook-secret} no esta configurado la validacion se omite.
 */
@RestController
@RequestMapping("/api/pagos/mercadopago/webhook")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    private final PagoWebhookService pagoWebhookService;
    private final MercadoPagoSignatureVerifier signatureVerifier;

    @PostMapping
    public ResponseEntity<Void> recibirNotificacion(
            @RequestHeader(name = "x-signature", required = false) String xSignature,
            @RequestHeader(name = "x-request-id", required = false) String xRequestId,
            @RequestParam(name = "topic", required = false) String topicParam,
            @RequestParam(name = "type", required = false) String typeParam,
            @RequestParam(name = "id", required = false) String idParam,
            @RequestParam(name = "data.id", required = false) String dataIdParam,
            @RequestBody(required = false) MercadoPagoWebhookPayload payload) {

        String tipo = (payload != null && payload.getType() != null) ? payload.getType()
                : (typeParam != null ? typeParam : topicParam);
        String paymentId = (payload != null && payload.getData() != null) ? payload.getData().getId() : idParam;

        if (!"payment".equalsIgnoreCase(tipo) || paymentId == null || paymentId.isBlank()) {
            log.info("Notificacion de Mercado Pago ignorada (tipo={}, id={})", tipo, paymentId);
            return ResponseEntity.ok().build();
        }

        String dataIdParaFirma = (dataIdParam != null && !dataIdParam.isBlank()) ? dataIdParam
                : (idParam != null && !idParam.isBlank() ? idParam : paymentId);
        if (!signatureVerifier.esValida(xSignature, xRequestId, dataIdParaFirma)) {
            log.warn("Notificacion de Mercado Pago descartada por firma invalida (paymentId={})", paymentId);
            return ResponseEntity.ok().build();
        }

        try {
            pagoWebhookService.procesarNotificacion(paymentId);
        } catch (Exception e) {
            log.error("Error procesando notificacion de Mercado Pago (paymentId={}): {}", paymentId, e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
