package com.inklusport.subscriptions.mercadopago;

import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.exception.PagoGatewayException;
import com.inklusport.subscriptions.service.PaymentGatewayClient;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementacion real de PaymentGatewayClient contra la API de Mercado Pago (Checkout Pro).
 * El webhook nunca confia en el payload recibido: siempre vuelve a consultar el pago aqui
 * (consultarPago) antes de marcarlo APROBADO/RECHAZADO, tal como recomienda Mercado Pago.
 */
@Service
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
                    .externalReference(referenciaExterna);

            // auto_return hace que Mercado Pago redirija solo a back_urls.success al aprobarse.
            // Pero MP descarta las back_urls que no sean publicas (http://localhost) y entonces
            // rechaza la preferencia con "auto_return invalid". Solo se activa si la URL es https.
            if (backUrlSuccess != null && backUrlSuccess.startsWith("https://")) {
                requestBuilder.autoReturn("approved");
            } else {
                log.warn("back-url-success no es https ({}). Se omite auto_return: tras pagar, el usuario "
                        + "debera pulsar 'Volver al sitio' en Mercado Pago.", backUrlSuccess);
            }

            if (notificationUrl != null && !notificationUrl.isBlank()) {
                requestBuilder.notificationUrl(notificationUrl);
            }

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(requestBuilder.build());

            return PaymentPreferenceResult.builder()
                    .preferenceId(preference.getId())
                    .checkoutUrl(preference.getInitPoint())
                    .build();
        } catch (MPApiException e) {
            log.error("Error de la API de Mercado Pago al crear preferencia: {}", e.getApiResponse().getContent());
            throw new PagoGatewayException("No se pudo crear la preferencia de pago en Mercado Pago", e);
        } catch (MPException e) {
            log.error("Error al crear preferencia en Mercado Pago: {}", e.getMessage());
            throw new PagoGatewayException("No se pudo crear la preferencia de pago en Mercado Pago", e);
        }
    }

    @Override
    public PaymentStatusResult consultarPago(String paymentIdExterno) {
        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(paymentIdExterno));

            return PaymentStatusResult.builder()
                    .paymentIdExterno(String.valueOf(payment.getId()))
                    .referenciaExterna(payment.getExternalReference())
                    .estado(mapEstado(payment.getStatus()))
                    .montoPagado(payment.getTransactionAmount())
                    .metodoPago(payment.getPaymentMethodId())
                    .build();
        } catch (MPApiException e) {
            log.error("Error de la API de Mercado Pago al consultar pago {}: {}", paymentIdExterno,
                    e.getApiResponse().getContent());
            throw new PagoGatewayException("No se pudo consultar el pago " + paymentIdExterno + " en Mercado Pago", e);
        } catch (MPException e) {
            log.error("Error al consultar pago {} en Mercado Pago: {}", paymentIdExterno, e.getMessage());
            throw new PagoGatewayException("No se pudo consultar el pago " + paymentIdExterno + " en Mercado Pago", e);
        }
    }

    @Override
    public PaymentStatusResult procesarPago(String cardToken, BigDecimal monto, String referenciaExterna,
                                             String descripcion, Integer cuotas, String paymentMethodId,
                                             String payerEmail, String docType, String docNumber) {
        try {
            IdentificationRequest identification = IdentificationRequest.builder()
                    .type(docType)
                    .number(docNumber)
                    .build();

            PaymentPayerRequest payer = PaymentPayerRequest.builder()
                    .email(payerEmail)
                    .identification(identification)
                    .build();

            PaymentCreateRequest.PaymentCreateRequestBuilder requestBuilder = PaymentCreateRequest.builder()
                    .transactionAmount(monto)
                    .token(cardToken)
                    .description(descripcion)
                    .installments(cuotas)
                    .paymentMethodId(paymentMethodId)
                    .payer(payer)
                    .externalReference(referenciaExterna);

            if (notificationUrl != null && !notificationUrl.isBlank()) {
                requestBuilder.notificationUrl(notificationUrl);
            }

            PaymentClient client = new PaymentClient();
            Payment payment = client.create(requestBuilder.build());

            return PaymentStatusResult.builder()
                    .paymentIdExterno(String.valueOf(payment.getId()))
                    .referenciaExterna(payment.getExternalReference())
                    .estado(mapEstado(payment.getStatus()))
                    .montoPagado(payment.getTransactionAmount())
                    .metodoPago(payment.getPaymentMethodId())
                    .build();
        } catch (MPApiException e) {
            log.error("Error de la API de Mercado Pago al procesar pago con tarjeta: {}", e.getApiResponse().getContent());
            throw new PagoGatewayException("No se pudo procesar el pago con Mercado Pago", e);
        } catch (MPException e) {
            log.error("Error al procesar pago con tarjeta en Mercado Pago: {}", e.getMessage());
            throw new PagoGatewayException("No se pudo procesar el pago con Mercado Pago", e);
        }
    }

    private EstadoPago mapEstado(String mpStatus) {
        if (mpStatus == null) {
            return EstadoPago.PENDIENTE;
        }
        return switch (mpStatus) {
            case "approved" -> EstadoPago.APROBADO;
            case "rejected", "cancelled", "refunded", "charged_back" -> EstadoPago.RECHAZADO;
            default -> EstadoPago.PENDIENTE; // pending, in_process, in_mediation, authorized
        };
    }
}
