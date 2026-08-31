package com.inklusport.suscripciones.mercadopago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resultado de crear una preferencia de pago (checkout) en la pasarela. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPreferenceResult {
    private String preferenceId;
    private String checkoutUrl;
}
