package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.dto.PagoSuscripcionResponse;
import com.inklusport.subscriptions.service.OrganizerIdentityService;
import com.inklusport.subscriptions.service.PagoSuscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/pagos/suscripciones")
@RequiredArgsConstructor
public class PagoSuscripcionController {

    private final PagoSuscripcionService pagoSuscripcionService;
    private final OrganizerIdentityService organizerIdentityService;

    @GetMapping("/historial")
    public ResponseEntity<List<PagoSuscripcionResponse>> historial(@AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(pagoSuscripcionService.listarPorOrganizador(
                organizerIdentityService.resolveUserId(principal)));
    }

    @GetMapping("/{pagoId}/comprobante")
    public ResponseEntity<Resource> descargarComprobante(@AuthenticationPrincipal String principal,
                                                         Authentication authentication,
                                                         @PathVariable Long pagoId) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        File archivo = pagoSuscripcionService.obtenerComprobante(
                pagoId, organizerIdentityService.resolveUserId(principal), esAdmin);
        Resource recurso = new FileSystemResource(archivo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getName() + "\"")
                .body(recurso);
    }
}
