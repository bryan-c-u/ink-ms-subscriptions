package com.inklusport.subscriptions.mercadopago;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * RF70 (robustez): valida la firma HMAC con la que Mercado Pago firma cada webhook.
 *
 * Mercado Pago envia dos cabeceras:
 *   - {@code x-signature}: {@code ts=<epoch>,v1=<hmacSha256Hex>}
 *   - {@code x-request-id}: id de la peticion
 * y el id del recurso llega como query param {@code data.id}. El manifiesto que se
 * firma es {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;} con HMAC-SHA256 y
 * la "clave secreta" del panel de MP (Webhooks / Notificaciones).
 *
 * Si {@code mercadopago.webhook-secret} no esta configurado la validacion se omite
 * (se acepta la notificacion) para no romper sandbox/demo; en produccion DEBE fijarse.
 * El resto del flujo ya no confia en el payload: siempre re-consulta el pago contra
 * la API de MP, por lo que esto es una capa extra, no la unica defensa.
 *
 * Doc: https://www.mercadopago.com.co/developers/es/docs/your-integrations/notifications/webhooks
 */
@Component
@Slf4j
public class MercadoPagoSignatureVerifier {

    private static final String HMAC_ALGO = "HmacSHA256";

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    public boolean estaConfigurado() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    /**
     * @param xSignature cabecera {@code x-signature}
     * @param xRequestId cabecera {@code x-request-id}
     * @param dataId     valor de {@code data.id} (query param) o, en su defecto, el id del pago
     * @return {@code true} si la firma es valida (o si no hay clave configurada);
     *         {@code false} si hay clave y la firma no coincide o falta.
     */
    public boolean esValida(String xSignature, String xRequestId, String dataId) {
        if (!estaConfigurado()) {
            log.warn("mercadopago.webhook-secret sin configurar: se acepta el webhook sin validar la firma "
                    + "(no usar en produccion)");
            return true;
        }
        if (xSignature == null || xSignature.isBlank()) {
            log.warn("Webhook de Mercado Pago sin cabecera x-signature; se descarta");
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String parte : xSignature.split(",")) {
            String[] kv = parte.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            switch (kv[0].trim()) {
                case "ts" -> ts = kv[1].trim();
                case "v1" -> v1 = kv[1].trim();
                default -> { /* ignora claves desconocidas */ }
            }
        }

        if (ts == null || v1 == null || v1.isBlank()) {
            log.warn("Cabecera x-signature de Mercado Pago con formato inesperado: {}", xSignature);
            return false;
        }

        StringBuilder manifest = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifest.append("id:").append(dataId.toLowerCase()).append(";");
        }
        if (xRequestId != null && !xRequestId.isBlank()) {
            manifest.append("request-id:").append(xRequestId).append(";");
        }
        manifest.append("ts:").append(ts).append(";");

        String calculada = hmacHex(manifest.toString());
        boolean ok = MessageDigest.isEqual(
                calculada.getBytes(StandardCharsets.UTF_8),
                v1.toLowerCase().getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            log.warn("Firma de webhook de Mercado Pago invalida (dataId={})", dataId);
        }
        return ok;
    }

    private String hmacHex(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el HMAC del webhook de Mercado Pago", e);
        }
    }
}
