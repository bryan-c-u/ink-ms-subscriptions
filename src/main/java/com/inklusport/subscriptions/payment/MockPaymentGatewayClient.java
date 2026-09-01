package com.inklusport.subscriptions.payment;

import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.service.PaymentGatewayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "app.payment.mode", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockPaymentGatewayClient implements PaymentGatewayClient {

    private final Map<String, String> paymentIdToReferencia = new ConcurrentHashMap<>();

    @Override
    public PaymentPreferenceResult crearPreferencia(String titulo, BigDecimal monto, String referenciaExterna) {
        String preferenceId = "MOCK-PREF-" + UUID.randomUUID();
        String paymentId = "MOCK-PAY-" + UUID.randomUUID();
        paymentIdToReferencia.put(preferenceId, referenciaExterna);
        paymentIdToReferencia.put(paymentId, referenciaExterna);
        paymentIdToReferencia.put(referenciaExterna, paymentId);
        log.info("Mock checkout: titulo={}, monto={}, ref={}, paymentId={}", titulo, monto, referenciaExterna, paymentId);
        return PaymentPreferenceResult.builder()
                .preferenceId(preferenceId)
                .checkoutUrl("mock://checkout/" + paymentId)
                .build();
    }

    @Override
    public PaymentStatusResult consultarPago(String paymentIdExterno) {
        String referencia = paymentIdToReferencia.getOrDefault(paymentIdExterno, paymentIdExterno);
        return PaymentStatusResult.builder()
                .paymentIdExterno(paymentIdExterno)
                .referenciaExterna(referencia)
                .estado(EstadoPago.APROBADO)
                .estadoPasarela("approved")
                .detalleEstado("accredited")
                .metodoPago("mock")
                .tipoPago("account_money")
                .build();
    }
}
