package com.inklusport.subscriptions.controller;

import com.inklusport.suscripciones.dto.ConfiguracionEventoPagoRequest;
import com.inklusport.suscripciones.dto.ConfiguracionEventoPagoResponse;
import com.inklusport.suscripciones.service.ConfiguracionEventoPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF65: configuracion de eventos como gratuitos o de pago. */
@RestController
@RequestMapping("/api/eventos-pago")
@RequiredArgsConstructor
public class ConfiguracionEventoPagoController {

    private final ConfiguracionEventoPagoService configuracionEventoPagoService;

    @PostMapping("/configuracion")
    public ResponseEntity<ConfiguracionEventoPagoResponse> configurar(@AuthenticationPrincipal String email,
                                                                        @Valid @RequestBody ConfiguracionEventoPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configuracionEventoPagoService.configurar(email, request));
    }

    @PutMapping("/configuracion/{eventoId}")
    public ResponseEntity<ConfiguracionEventoPagoResponse> actualizar(@AuthenticationPrincipal String email,
                                                                       @PathVariable String eventoId,
                                                                       @Valid @RequestBody ConfiguracionEventoPagoRequest request) {
        return ResponseEntity.ok(configuracionEventoPagoService.actualizar(email, eventoId, request));
    }

    @GetMapping("/configuracion/{eventoId}")
    public ResponseEntity<ConfiguracionEventoPagoResponse> obtener(@PathVariable String eventoId) {
        return ResponseEntity.ok(configuracionEventoPagoService.obtenerPorEvento(eventoId));
    }

    @GetMapping("/configuracion")
    public ResponseEntity<List<ConfiguracionEventoPagoResponse>> listarPropias(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(configuracionEventoPagoService.listarPorOrganizador(email));
    }
}
