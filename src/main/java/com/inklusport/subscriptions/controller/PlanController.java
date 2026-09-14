package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.dto.PlanResponse;
import com.inklusport.subscriptions.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF56: consulta de planes disponibles (Usuario / Organizador). */
@RestController
@RequestMapping("/api/planes")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<List<PlanResponse>> listar() {
        return ResponseEntity.ok(planService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(planService.obtenerPorId(id));
    }
}
