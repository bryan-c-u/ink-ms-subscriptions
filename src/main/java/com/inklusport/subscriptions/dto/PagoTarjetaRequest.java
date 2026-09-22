package com.inklusport.subscriptions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos que llegan del checkout propio (formulario embebido con el SDK JS de Mercado
 * Pago) para cobrar un pago ya creado (PENDIENTE). El numero de tarjeta y el CVV nunca
 * llegan aqui: {@code cardToken} lo genera el SDK en el navegador, tokenizado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoTarjetaRequest {

    @NotBlank
    private String cardToken;

    @NotNull(message = "Las cuotas son obligatorias")
    @Positive(message = "Las cuotas deben ser mayor que 0")
    private Integer installments;

    @NotBlank
    private String paymentMethodId;

    private String issuerId;

    @NotBlank
    private String docType;

    @NotBlank
    private String docNumber;
}
