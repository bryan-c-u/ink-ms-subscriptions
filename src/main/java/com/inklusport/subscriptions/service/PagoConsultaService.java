package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.PagoEstadoResponse;
import com.inklusport.subscriptions.dto.PagoTarjetaRequest;
import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.exception.PagoNotFoundException;
import com.inklusport.subscriptions.mercadopago.PaymentStatusResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RF70 (robustez): consulta bajo demanda del estado de un cobro, para la pagina de
 * retorno del checkout ({@code /pagos/exito|pendiente|error}). Si el pago sigue
 * PENDIENTE y el frontend aporta el {@code paymentId} de Mercado Pago (viene en los
 * query params de la back_url), se re-consulta el pago real contra la API de MP y se
 * aplica la confirmacion sin esperar al webhook.
 *
 * No depende de {@link PagoWebhookService} para no acoplar la ruta HTTP publica del
 * webhook con la consulta autenticada; reutiliza directamente los servicios de dominio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagoConsultaService {

    private final PaymentGatewayClient paymentGatewayClient;
    private final PagoSuscripcionService pagoSuscripcionService;
    private final PagoEventoService pagoEventoService;

    public PagoEstadoResponse consultarEstado(String email, String referencia, String paymentIdExterno,
                                              boolean esAdmin) {
        boolean esSuscripcion = referencia.startsWith(PagoSuscripcionService.PREFIJO_REFERENCIA);
        boolean esEvento = referencia.startsWith(PagoEventoService.PREFIJO_REFERENCIA);
        if (!esSuscripcion && !esEvento) {
            throw new PagoNotFoundException("Referencia de pago no reconocida: " + referencia);
        }

        PagoEstadoResponse estado = leer(esSuscripcion, email, referencia, esAdmin);

        if (estado.getEstado() != EstadoPago.PENDIENTE || paymentIdExterno == null || paymentIdExterno.isBlank()) {
            return estado;
        }

        boolean reconciliado = false;
        try {
            PaymentStatusResult status = paymentGatewayClient.consultarPago(paymentIdExterno);
            if (referencia.equals(status.getReferenciaExterna())) {
                if (esSuscripcion) {
                    pagoSuscripcionService.confirmarPago(status);
                } else {
                    pagoEventoService.confirmarPago(status);
                }
                reconciliado = true;
            } else {
                log.warn("El paymentId {} no corresponde a la referencia {} (external_reference={}); no se reconcilia",
                        paymentIdExterno, referencia, status.getReferenciaExterna());
            }
        } catch (RuntimeException e) {
            // Falla de red/gateway, o una notificacion del webhook que gano la carrera
            // (OptimisticLock): se ignora y se devuelve el estado que haya quedado en BD.
            log.warn("No se pudo reconciliar el pago {} contra Mercado Pago: {}", referencia, e.getMessage());
        }

        PagoEstadoResponse actualizado = leer(esSuscripcion, email, referencia, esAdmin);
        actualizado.setReconciliadoAhora(reconciliado);
        return actualizado;
    }

    /**
     * Checkout propio (RF70, sin interfaz de Mercado Pago): cobra un pago PENDIENTE con
     * el token de tarjeta que genero el formulario embebido. Soporta suscripciones
     * ({@code PS-}) e inscripciones a eventos ({@code PE-}).
     */
    public PagoEstadoResponse pagarConTarjeta(String email, String referencia, PagoTarjetaRequest datos,
                                               boolean esAdmin) {
        if (referencia.startsWith(PagoSuscripcionService.PREFIJO_REFERENCIA)) {
            return pagoSuscripcionService.pagarConTarjeta(email, referencia, datos, esAdmin);
        }
        if (referencia.startsWith(PagoEventoService.PREFIJO_REFERENCIA)) {
            return pagoEventoService.pagarConTarjeta(email, referencia, datos, esAdmin);
        }
        throw new PagoNotFoundException("Referencia de pago no reconocida: " + referencia);
    }

    private PagoEstadoResponse leer(boolean esSuscripcion, String email, String referencia, boolean esAdmin) {
        return esSuscripcion
                ? pagoSuscripcionService.estadoActual(email, referencia, esAdmin)
                : pagoEventoService.estadoActual(email, referencia, esAdmin);
    }
}
