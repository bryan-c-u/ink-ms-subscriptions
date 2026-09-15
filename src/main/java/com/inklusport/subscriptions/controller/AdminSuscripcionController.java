package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.dto.CambiarEstadoSuscripcionRequest;
import com.inklusport.subscriptions.dto.HistorialSuscripcionResponse;
import com.inklusport.subscriptions.dto.SuscripcionResponse;
import com.inklusport.subscriptions.service.SuscripcionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RF59: control administrativo del estado de suscripciones. RF63: historial por organizador.
 * Nota: se usa /api/suscripciones/admin (no /api/admin/suscripciones) por la misma
 * razon documentada en AdminPlanController (evitar colision con /api/admin/** de ink-ms-users
 * en el gateway).
 */
@RestController
@RequestMapping("/api/suscripciones/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSuscripcionController {

    private final SuscripcionService suscripcionService;

    @PatchMapping("/{id}/estado")
    public ResponseEntity<SuscripcionResponse> cambiarEstado(@PathVariable Long id,
                                                               @Valid @RequestBody CambiarEstadoSuscripcionRequest request) {
        return ResponseEntity.ok(suscripcionService.cambiarEstado(id, request.getEstado()));
    }

    @GetMapping("/organizadores/{organizadorId}/historial")
    public ResponseEntity<List<HistorialSuscripcionResponse>> historialPorOrganizador(@PathVariable String organizadorId) {
        return ResponseEntity.ok(suscripcionService.historialPorOrganizador(organizadorId));
    }

    /**
     * RF58: suscripciones del organizador para que el admin vea el estado vigente antes
     * de aplicar una transicion. Usa listarPropias (solo lectura) y no obtenerActual,
     * que le asignaria un plan gratuito automaticamente si el organizador aun no tiene
     * ninguna suscripcion.
     */
    @GetMapping("/organizadores/{organizadorId}/suscripciones")
    public ResponseEntity<List<SuscripcionResponse>> suscripcionesPorOrganizador(@PathVariable String organizadorId) {
        return ResponseEntity.ok(suscripcionService.listarPropias(organizadorId));
    }
}
