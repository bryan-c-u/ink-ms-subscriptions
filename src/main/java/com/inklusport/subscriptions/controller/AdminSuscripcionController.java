package com.inklusport.suscripciones.controller;

import com.inklusport.suscripciones.dto.CambiarEstadoSuscripcionRequest;
import com.inklusport.suscripciones.dto.HistorialSuscripcionResponse;
import com.inklusport.suscripciones.dto.SuscripcionResponse;
import com.inklusport.suscripciones.service.SuscripcionService;
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
}
