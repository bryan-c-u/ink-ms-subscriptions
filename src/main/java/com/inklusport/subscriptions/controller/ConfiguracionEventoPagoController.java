package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.dto.ConfiguracionEventoPagoRequest;
import com.inklusport.subscriptions.dto.ConfiguracionEventoPagoResponse;
import com.inklusport.subscriptions.service.ConfiguracionEventoPagoService;
import com.inklusport.subscriptions.service.OrganizerIdentityService;
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
    private final OrganizerIdentityService organizerIdentityService;

    @PostMapping("/configuracion")
    public ResponseEntity<ConfiguracionEventoPagoResponse> configurar(@AuthenticationPrincipal String principal,
                                                                        @Valid @RequestBody ConfiguracionEventoPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configuracionEventoPagoService.configurar(id(principal), request));
    }

    @PutMapping("/configuracion/{eventoId}")
    public ResponseEntity<ConfiguracionEventoPagoResponse> actualizar(@AuthenticationPrincipal String principal,
                                                                       @PathVariable String eventoId,
                                                                       @Valid @RequestBody ConfiguracionEventoPagoRequest request) {
        return ResponseEntity.ok(configuracionEventoPagoService.actualizar(id(principal), eventoId, request));
    }

    @GetMapping("/configuracion/{eventoId}")
    public ResponseEntity<ConfiguracionEventoPagoResponse> obtener(@PathVariable String eventoId) {
        return ResponseEntity.ok(configuracionEventoPagoService.obtenerPorEvento(eventoId));
    }

    @GetMapping("/configuracion")
    public ResponseEntity<List<ConfiguracionEventoPagoResponse>> listarPropias(@AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(configuracionEventoPagoService.listarPorOrganizador(id(principal)));
    }

    private String id(String principal) {
        return organizerIdentityService.resolveUserId(principal);
    }
}
