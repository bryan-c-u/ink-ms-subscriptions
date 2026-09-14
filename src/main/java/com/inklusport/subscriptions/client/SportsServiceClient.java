package com.inklusport.subscriptions.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Confirma en sports-ms la inscripción tras un pago de evento aprobado.
 */
@Component
@Slf4j
public class SportsServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sports.service.url:http://localhost:3003}")
    private String sportsServiceUrl;

    public void confirmarInscripcionPagada(String usuarioId, String eventoId) {
        if (usuarioId == null || eventoId == null) {
            return;
        }
        try {
            String url = sportsServiceUrl
                    + "/api/internal/registrations/eventos/" + eventoId
                    + "/usuarios/" + usuarioId + "/pago-confirmado";
            restTemplate.exchange(url, HttpMethod.POST, HttpEntity.EMPTY, Void.class);
        } catch (Exception e) {
            log.error("No se pudo confirmar la inscripción pagada de {} en evento {}: {}",
                    usuarioId, eventoId, e.getMessage());
        }
    }
}
