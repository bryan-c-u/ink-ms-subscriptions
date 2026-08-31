package com.inklusport.suscripciones.mercadopago;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Inicializa el SDK de Mercado Pago con el access token configurado (sandbox o produccion). */
@Component
@Slf4j
public class MercadoPagoClientConfig {

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @PostConstruct
    public void init() {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("mercadopago.access-token no esta configurado. Los cobros via Mercado Pago fallaran.");
            return;
        }
        com.mercadopago.MercadoPagoConfig.setAccessToken(accessToken);
    }
}
