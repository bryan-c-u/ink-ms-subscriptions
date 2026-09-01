package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.dto.*;
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

    @PostMapping
    public ResponseEntity<PagoCheckoutResponse> crear(@AuthenticationPrincipal String email,
                                                        @Valid @RequestBody CrearSuscripcionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(suscripcionService.crearSolicitud(email, request));
    }

    @GetMapping("/actual")
    public ResponseEntity<SuscripcionResponse> actual(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(suscripcionService.obtenerActual(email));
    }

    @GetMapping
    public ResponseEntity<List<SuscripcionResponse>> listarPropias(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(suscripcionService.listarPropias(email));
    }

    @PostMapping("/{id}/renovar")
    public ResponseEntity<PagoCheckoutResponse> renovar(@AuthenticationPrincipal String email,
                                                          @PathVariable Long id,
                                                          @RequestBody(required = false) RenovarSuscripcionRequest request) {
        RenovarSuscripcionRequest body = request != null ? request : new RenovarSuscripcionRequest();
        return ResponseEntity.ok(suscripcionService.renovar(email, id, body));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialSuscripcionResponse>> historial(@AuthenticationPrincipal String email,
                                                                          @PathVariable Long id) {
        return ResponseEntity.ok(suscripcionService.historialPropio(email, id));
    }

    @GetMapping("/{id}/pagos")
    public ResponseEntity<List<PagoSuscripcionResponse>> pagos(@AuthenticationPrincipal String email,
                                                                 @PathVariable Long id) {
        suscripcionService.obtenerPropia(email, id);
        return ResponseEntity.ok(pagoSuscripcionService.listarPorSuscripcion(id));
    }
}
