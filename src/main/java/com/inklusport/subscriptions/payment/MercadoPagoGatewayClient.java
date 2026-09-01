package com.inklusport.subscriptions.payment;

import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.exception.PagoGatewayException;
import com.inklusport.subscriptions.service.PaymentGatewayClient;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.payment.mode", havingValue = "mercadopago")
@Slf4j
public class MercadoPagoGatewayClient implements PaymentGatewayClient {

    @Value("${app.mercadopago.currency:COP}")
    private String currency;

    @Value("${app.mercadopago.back-url-success:http://localhost:4200/pagos/exito}")
    private String backUrlSuccess;

    @Value("${app.mercadopago.back-url-pending:http://localhost:4200/pagos/pendiente}")
    private String backUrlPending;

    @Value("${app.mercadopago.back-url-failure:http://localhost:4200/pagos/error}")
    private String backUrlFailure;

    @Value("${app.mercadopago.notification-url:}")
    private String notificationUrl;

    @Override
    public PaymentPreferenceResult crearPreferencia(String titulo, BigDecimal monto, String referenciaExterna) {
        try {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(titulo)
                    .quantity(1)
                    .currencyId(currency)
                    .unitPrice(monto)
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(backUrlSuccess)
                    .pending(backUrlPending)
                    .failure(backUrlFailure)
                    .build();

            PreferenceRequest.PreferenceRequestBuilder requestBuilder = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference(referenciaExterna);

            if (notificationUrl != null && !notificationUrl.isBlank()) {
                requestBuilder.notificationUrl(notificationUrl);
            }

            Preference preference = new PreferenceClient().create(requestBuilder.build());
            return PaymentPreferenceResult.builder()
                    .preferenceId(preference.getId())
                    .checkoutUrl(preference.getInitPoint())
                    .build();
        } catch (MPApiException e) {
            log.error("API Mercado Pago al crear preferencia: {}", e.getApiResponse().getContent());
            throw new PagoGatewayException("No se pudo crear la preferencia de pago en Mercado Pago", e);
        } catch (MPException e) {
            log.error("Error Mercado Pago al crear preferencia: {}", e.getMessage());
            throw new PagoGatewayException("No se pudo crear la preferencia de pago en Mercado Pago", e);
        }
    }

    @Override
    public PaymentStatusResult consultarPago(String paymentIdExterno) {
        try {
            Payment payment = new PaymentClient().get(Long.parseLong(paymentIdExterno));
            return PaymentStatusResult.builder()
                    .paymentIdExterno(String.valueOf(payment.getId()))
                    .referenciaExterna(payment.getExternalReference())
                    .estado(mapEstado(payment.getStatus()))
                    .estadoPasarela(payment.getStatus())
                    .detalleEstado(payment.getStatusDetail())
                    .montoPagado(payment.getTransactionAmount())
                    .metodoPago(payment.getPaymentMethodId())
                    .tipoPago(payment.getPaymentTypeId())
                    .build();
        } catch (MPApiException e) {
            log.error("API Mercado Pago al consultar pago {}: {}", paymentIdExterno, e.getApiResponse().getContent());
            throw new PagoGatewayException("No se pudo consultar el pago " + paymentIdExterno, e);
        } catch (MPException e) {
            log.error("Error Mercado Pago al consultar pago {}: {}", paymentIdExterno, e.getMessage());
            throw new PagoGatewayException("No se pudo consultar el pago " + paymentIdExterno, e);
        }
    }

    private EstadoPago mapEstado(String mpStatus) {
        if (mpStatus == null) {
            return EstadoPago.PENDIENTE;
        }
        return switch (mpStatus) {
            case "approved" -> EstadoPago.APROBADO;
            case "rejected" -> EstadoPago.RECHAZADO;
            case "cancelled" -> EstadoPago.CANCELADO;
            case "refunded", "charged_back" -> EstadoPago.REEMBOLSADO;
            default -> EstadoPago.PENDIENTE;
        };
    }
}
