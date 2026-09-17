package com.inklusport.subscriptions.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Confirma en sports-ms la inscripción tras un pago de evento aprobado.
 */
@Component
@Slf4j
public class SportsServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sports.service.url:http://localhost:3003}")
    private String sportsServiceUrl;

    /**
     * Nombre del evento para el snapshot de {@code pago_evento.nombre_evento} (RF66).
     * Si sports-ms no responde, el pago se crea igual y el historial muestra el id.
     *
     * @param eventoId identificador del evento
     * @return nombre, o {@code null}
     */
    public String obtenerNombreEvento(String eventoId) {
        if (eventoId == null || eventoId.isBlank()) {
            return null;
        }
        try {
            Map<?, ?> body = restTemplate.getForObject(
                    sportsServiceUrl + "/api/events/{id}", Map.class, eventoId);
            if (body == null) {
                return null;
            }
            Object nombre = body.get("name");
            if (nombre == null) {
                return null;
            }
            String value = String.valueOf(nombre).trim();
            return value.isEmpty() ? null : value;
        } catch (Exception e) {
            log.debug("No se resolvió el nombre del evento {}: {}", eventoId, e.getMessage());
            return null;
        }
    }

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
