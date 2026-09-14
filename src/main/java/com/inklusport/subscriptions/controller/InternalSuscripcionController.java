package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.dto.PuedeCrearEventoResponse;
import com.inklusport.subscriptions.dto.SuscripcionResponse;
import com.inklusport.subscriptions.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints internos servicio-a-servicio (sin JWT, ver SecurityConfig / JwtAuthenticationFilter).
 * RF66: asignacion del plan gratuito al registrar un organizador nuevo (a invocar desde
 * ink-ms-auth/ink-ms-users cuando se cablee esa integracion).
 * RF60: control de limites de eventos por mes, a invocar desde el futuro ink-ms-eventos.
 */
@RestController
@RequestMapping("/api/internal/suscripciones")
@RequiredArgsConstructor
public class InternalSuscripcionController {

    private final SuscripcionService suscripcionService;

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
}
