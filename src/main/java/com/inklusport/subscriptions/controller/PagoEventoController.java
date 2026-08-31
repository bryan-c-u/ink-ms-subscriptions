package com.inklusport.suscripciones.controller;

import com.inklusport.suscripciones.dto.PagoCheckoutResponse;
import com.inklusport.suscripciones.dto.PagoEventoResponse;
import com.inklusport.suscripciones.service.PagoEventoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/{eventoId}/inscripcion")
    public ResponseEntity<PagoCheckoutResponse> inscribirse(@AuthenticationPrincipal String email,
                                                              @PathVariable String eventoId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoEventoService.inscribirse(email, eventoId));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<PagoEventoResponse>> historial(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(pagoEventoService.historialUsuario(email));
    }

    @GetMapping("/{pagoId}/comprobante")
    public ResponseEntity<Resource> descargarComprobante(@AuthenticationPrincipal String email,
                                                           Authentication authentication,
                                                           @PathVariable Long pagoId) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        File archivo = pagoEventoService.obtenerComprobante(pagoId, email, esAdmin);
        Resource recurso = new FileSystemResource(archivo);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getName() + "\"")
                .body(recurso);
    }
}
