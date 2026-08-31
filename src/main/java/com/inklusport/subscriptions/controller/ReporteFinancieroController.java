package com.inklusport.suscripciones.controller;

import com.inklusport.suscripciones.dto.ReporteFinancieroResponse;
import com.inklusport.suscripciones.service.ReporteFinancieroService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** RF64: reportes financieros para organizador (sus propios eventos) y para administrador (globales). */
@RestController
@RequiredArgsConstructor
public class ReporteFinancieroController {

    private final ReporteFinancieroService reporteFinancieroService;

    @GetMapping("/api/reportes/financiero")
    public ResponseEntity<ReporteFinancieroResponse> reportePropio(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDateTime[] rango = normalizarRango(desde, hasta);
        return ResponseEntity.ok(reporteFinancieroService.reporteOrganizador(email, rango[0], rango[1]));
    }

    // /api/reportes/admin (no /api/admin/reportes): /api/admin/** ya esta reservado
    // para ink-ms-users en el gateway, ver nota en AdminPlanController.
    @GetMapping("/api/reportes/admin/financiero")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReporteFinancieroResponse> reporteGlobal(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDateTime[] rango = normalizarRango(desde, hasta);
        return ResponseEntity.ok(reporteFinancieroService.reporteAdmin(rango[0], rango[1]));
    }

    private LocalDateTime[] normalizarRango(LocalDate desde, LocalDate hasta) {
        LocalDate finalHasta = hasta != null ? hasta : LocalDate.now();
        LocalDate finalDesde = desde != null ? desde : finalHasta.minusDays(30);
        return new LocalDateTime[]{finalDesde.atStartOfDay(), LocalDateTime.of(finalHasta, LocalTime.MAX)};
    }
}
