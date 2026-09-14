package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.dto.PuedeCrearEventoResponse;
import com.inklusport.subscriptions.dto.SuscripcionResponse;
import com.inklusport.subscriptions.exception.ConfiguracionEventoPagoNotFoundException;
import com.inklusport.subscriptions.service.ConfiguracionEventoPagoService;
import com.inklusport.subscriptions.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints internos servicio-a-servicio (sin JWT, ver SecurityConfig / JwtAuthenticationFilter).
 */
@RestController
@RequestMapping("/api/internal/suscripciones")
@RequiredArgsConstructor
public class InternalSuscripcionController {

    private final SuscripcionService suscripcionService;
    private final ConfiguracionEventoPagoService configuracionEventoPagoService;

    @PostMapping("/organizadores/{organizadorId}/plan-gratuito")
    public ResponseEntity<SuscripcionResponse> asignarPlanGratuito(@PathVariable String organizadorId) {
        return ResponseEntity.ok(suscripcionService.asignarPlanGratuito(organizadorId));
    }

    @GetMapping("/organizadores/{organizadorId}/puede-crear-evento")
    public ResponseEntity<PuedeCrearEventoResponse> puedeCrearEvento(@PathVariable String organizadorId) {
        return ResponseEntity.ok(suscripcionService.puedeCrearEvento(organizadorId));
    }

    @PostMapping("/organizadores/{organizadorId}/registrar-evento-creado")
    public ResponseEntity<Void> registrarEventoCreado(@PathVariable String organizadorId) {
        suscripcionService.registrarEventoCreado(organizadorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/eventos/{eventoId}/pago")
    public ResponseEntity<Map<String, Object>> configuracionPago(@PathVariable String eventoId) {
        try {
            var config = configuracionEventoPagoService.obtenerPorEvento(eventoId);
            return ResponseEntity.ok(Map.of(
                    "esPago", Boolean.TRUE.equals(config.getEsPago()),
                    "valorInscripcion", config.getValorInscripcion() != null ? config.getValorInscripcion() : 0
            ));
        } catch (ConfiguracionEventoPagoNotFoundException e) {
            return ResponseEntity.ok(Map.of("esPago", false));
        }
    }
}
