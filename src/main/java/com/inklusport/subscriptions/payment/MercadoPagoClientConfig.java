package com.inklusport.subscriptions.payment;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.payment.mode", havingValue = "mercadopago")
@Slf4j
public class MercadoPagoClientConfig {

    @Value("${app.mercadopago.access-token:}")
    private String accessToken;

    @PostConstruct
    public void init() {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("MERCADOPAGO_ACCESS_TOKEN no está configurado. Los cobros reales fallarán.");
            return;
        }
        com.mercadopago.MercadoPagoConfig.setAccessToken(accessToken);
        log.info("SDK de Mercado Pago inicializado");
    }
}
