package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.dto.*;
import com.inklusport.subscriptions.service.OrganizerIdentityService;
import com.inklusport.subscriptions.service.PagoSuscripcionService;
import com.inklusport.subscriptions.service.SuscripcionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF58, RF61, RF63: gestion de la suscripcion propia del organizador autenticado. */
@RestController
@RequestMapping("/api/suscripciones")
@RequiredArgsConstructor
public class SuscripcionController {

    private final SuscripcionService suscripcionService;
    private final PagoSuscripcionService pagoSuscripcionService;
    private final OrganizerIdentityService organizerIdentityService;

    @PostMapping
    public ResponseEntity<PagoCheckoutResponse> crear(@AuthenticationPrincipal String principal,
                                                        @Valid @RequestBody CrearSuscripcionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(suscripcionService.crearSolicitud(id(principal), request));
    }

    @GetMapping("/actual")
    public ResponseEntity<SuscripcionResponse> actual(@AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(suscripcionService.obtenerActual(id(principal)));
    }

    @GetMapping
    public ResponseEntity<List<SuscripcionResponse>> listarPropias(@AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(suscripcionService.listarPropias(id(principal)));
    }

    @PostMapping("/{id}/renovar")
    public ResponseEntity<PagoCheckoutResponse> renovar(@AuthenticationPrincipal String principal,
                                                          @PathVariable Long id,
                                                          @RequestBody(required = false) RenovarSuscripcionRequest request) {
        RenovarSuscripcionRequest body = request != null ? request : new RenovarSuscripcionRequest();
        return ResponseEntity.ok(suscripcionService.renovar(id(principal), id, body));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialSuscripcionResponse>> historial(@AuthenticationPrincipal String principal,
                                                                          @PathVariable Long id) {
        return ResponseEntity.ok(suscripcionService.historialPropio(id(principal), id));
    }

    @GetMapping("/{id}/pagos")
    public ResponseEntity<List<PagoSuscripcionResponse>> pagos(@AuthenticationPrincipal String principal,
                                                                 @PathVariable Long id) {
        suscripcionService.obtenerPropia(id(principal), id);
        return ResponseEntity.ok(pagoSuscripcionService.listarPorSuscripcion(id));
    }

    private String id(String principal) {
        return organizerIdentityService.resolveUserId(principal);
    }
}
