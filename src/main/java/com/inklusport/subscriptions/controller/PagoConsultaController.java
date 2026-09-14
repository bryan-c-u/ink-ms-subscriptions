package com.inklusport.subscriptions.controller;

import com.inklusport.subscriptions.dto.PagoEstadoResponse;
import com.inklusport.subscriptions.dto.PagoTarjetaRequest;
import com.inklusport.subscriptions.service.PagoConsultaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * RF70 (robustez): la pagina de retorno del checkout consulta aqui el estado del cobro
 * para no depender unicamente del webhook.
 *
 * {@code GET /api/pagos/{referencia}/estado?paymentId=<id de Mercado Pago>}
 *
 * {@code referencia} es la {@code external_reference} que devuelve la back_url
 * (prefijo {@code PS-} suscripcion / {@code PE-} evento). {@code paymentId} es opcional:
 * si se envia y el pago sigue PENDIENTE, se fuerza la consulta contra Mercado Pago.
 */
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoConsultaController {

    private final PagoConsultaService pagoConsultaService;

    @GetMapping("/{referencia}/estado")
    public ResponseEntity<PagoEstadoResponse> consultarEstado(
            @AuthenticationPrincipal String email,
            Authentication authentication,
            @PathVariable String referencia,
            @RequestParam(name = "paymentId", required = false) String paymentId) {

        boolean esAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(pagoConsultaService.consultarEstado(email, referencia, paymentId, esAdmin));
    }

    /**
     * RF70 (checkout propio): cobra un pago PENDIENTE con el token de tarjeta que genero
     * el formulario embebido (SDK JS de Mercado Pago). El numero de tarjeta y el CVV
     * nunca llegan a este endpoint.
     */
    @PostMapping("/{referencia}/pagar-tarjeta")
    public ResponseEntity<PagoEstadoResponse> pagarConTarjeta(
            @AuthenticationPrincipal String email,
            Authentication authentication,
            @PathVariable String referencia,
            @Valid @RequestBody PagoTarjetaRequest request) {

        boolean esAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(pagoConsultaService.pagarConTarjeta(email, referencia, request, esAdmin));
    }
}
