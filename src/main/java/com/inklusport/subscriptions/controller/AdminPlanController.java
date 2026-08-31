package com.inklusport.suscripciones.controller;

import com.inklusport.suscripciones.dto.PlanRequest;
import com.inklusport.suscripciones.dto.PlanResponse;
import com.inklusport.suscripciones.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RF67: administracion de planes de suscripcion.
 * Nota: se usa /api/planes/admin (no /api/admin/planes) porque en el gateway
 * /api/admin/** ya esta reservado para ink-ms-users; anidar bajo /api/planes/**
 * evita esa colision de rutas.
 */
@RestController
@RequestMapping("/api/planes/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlanController {

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<List<PlanResponse>> listarTodos() {
        return ResponseEntity.ok(planService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<PlanResponse> crear(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> actualizar(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.actualizar(id, request));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<PlanResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(planService.desactivar(id));
    }
}
