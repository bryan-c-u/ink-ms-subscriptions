package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.entity.WebhookPasarela;
import com.inklusport.subscriptions.enums.Pasarela;
import com.inklusport.subscriptions.payment.PaymentStatusResult;
import com.inklusport.subscriptions.repository.TransaccionPasarelaRepository;
import com.inklusport.subscriptions.repository.WebhookPasarelaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoWebhookService {

    private final PaymentGatewayClient paymentGatewayClient;
    private final PagoSuscripcionService pagoSuscripcionService;
    private final PagoEventoService pagoEventoService;
    private final WebhookPasarelaRepository webhookPasarelaRepository;
    private final TransaccionPasarelaRepository transaccionPasarelaRepository;

    @Transactional
    public void procesarNotificacion(String paymentIdExterno) {
        WebhookPasarela logWebhook = new WebhookPasarela();
        logWebhook.setPasarela(Pasarela.MERCADOPAGO);
        logWebhook.setTipoNotificacion("payment");
        logWebhook.setIdExterno(paymentIdExterno);
        logWebhook.setPayload("{\"id\":\"" + paymentIdExterno + "\"}");
        logWebhook.setProcesado(false);

        try {
            PaymentStatusResult status = paymentGatewayClient.consultarPago(paymentIdExterno);
            String referencia = status.getReferenciaExterna();
            if (referencia == null || referencia.isBlank()) {
                logWebhook.setResultado("sin referencia externa");
                webhookPasarelaRepository.save(logWebhook);
                return;
            }

            transaccionPasarelaRepository.findByReferenciaExterna(referencia)
                    .ifPresent(logWebhook::setTransaccion);

            if (referencia.startsWith(PagoSuscripcionService.PREFIJO_REFERENCIA)) {
                pagoSuscripcionService.confirmarPago(status);
            } else if (referencia.startsWith(PagoEventoService.PREFIJO_REFERENCIA)) {
                pagoEventoService.confirmarPago(status);
            } else {
                log.warn("Referencia externa con prefijo desconocido: {}", referencia);
            }
            logWebhook.setProcesado(true);
            logWebhook.setResultado("ok");
            logWebhook.setFechaProceso(LocalDateTime.now());
        } catch (Exception e) {
            logWebhook.setResultado(e.getMessage());
            log.error("Error procesando webhook {}: {}", paymentIdExterno, e.getMessage());
        }
        webhookPasarelaRepository.save(logWebhook);
    }
}
