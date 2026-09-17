package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.dto.PagoCheckoutResponse;
import com.inklusport.subscriptions.dto.PagoEventoResponse;
import com.inklusport.subscriptions.service.OrganizerIdentityService;
import com.inklusport.subscriptions.service.PagoEventoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

/** RF57, RF68, RF69: inscripcion pagada a eventos, historial de pagos y descarga de comprobante. */
@RestController
@RequestMapping("/api/pagos/eventos")
@RequiredArgsConstructor
public class PagoEventoController {

    private final PagoEventoService pagoEventoService;
    private final OrganizerIdentityService organizerIdentityService;

    @PostMapping("/{eventoId}/inscripcion")
    public ResponseEntity<PagoCheckoutResponse> inscribirse(@AuthenticationPrincipal String principal,
                                                              @PathVariable String eventoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoEventoService.inscribirse(id(principal), eventoId));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<PagoEventoResponse>> historial(@AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(pagoEventoService.historialUsuario(id(principal)));
    }

    @GetMapping("/recibidos")
    public ResponseEntity<List<PagoEventoResponse>> historialOrganizador(@AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(pagoEventoService.historialOrganizador(id(principal)));
    }

    @GetMapping("/{pagoId}/comprobante")
    public ResponseEntity<Resource> descargarComprobante(@AuthenticationPrincipal String principal,
                                                           Authentication authentication,
                                                           @PathVariable Long pagoId) {
        boolean esAdmin = esAdmin(authentication);
        File archivo = pagoEventoService.obtenerComprobante(pagoId, id(principal), esAdmin);
        return pdf(archivo);
    }

    private String id(String principal) {
        return organizerIdentityService.resolveUserId(principal);
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private ResponseEntity<Resource> pdf(File archivo) {
        Resource recurso = new FileSystemResource(archivo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getName() + "\"")
                .body(recurso);
    }
}
