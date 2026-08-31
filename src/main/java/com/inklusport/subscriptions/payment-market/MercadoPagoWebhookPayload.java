package com.inklusport.suscripciones.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Notificacion IPN/webhook que envia Mercado Pago. Solo se usa "type" y "data.id" para
 * volver a consultar el pago real contra la API (no se confia en el resto del payload).
 * Doc: https://www.mercadopago.com.co/developers/es/docs/checkout-api/webhooks
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MercadoPagoWebhookPayload {

    private String type;

    private String action;

    @JsonProperty("data")
    private WebhookData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookData {
        private String id;
    }
}
