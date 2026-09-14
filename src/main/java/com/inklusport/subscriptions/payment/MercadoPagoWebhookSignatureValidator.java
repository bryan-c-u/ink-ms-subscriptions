package com.inklusport.subscriptions.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Valida el encabezado x-signature de webhooks de Mercado Pago.
 */
@Component
@Slf4j
public class MercadoPagoWebhookSignatureValidator {

    public boolean isValid(String secret, String xSignature, String xRequestId, String dataId) {
        if (secret == null || secret.isBlank()) {
            log.debug("MERCADOPAGO_WEBHOOK_SECRET vacío; se omite validación de firma");
            return true;
        }
        if (xSignature == null || xSignature.isBlank() || dataId == null || dataId.isBlank()) {
            return false;
        }

        String ts = extract(xSignature, "ts");
        String hash = extract(xSignature, "v1");
        if (ts == null || hash == null) {
            return false;
        }

        String requestId = xRequestId == null ? "" : xRequestId;
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(
                    expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                    hash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("No se pudo validar la firma del webhook: {}", e.getMessage());
            return false;
        }
    }

    private static String extract(String header, String key) {
        for (String part : header.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && key.equalsIgnoreCase(kv[0].trim())) {
                return kv[1].trim();
            }
        }
        return null;
    }
}
