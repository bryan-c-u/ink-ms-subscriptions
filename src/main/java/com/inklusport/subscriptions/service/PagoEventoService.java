package com.inklusport.suscripciones.service;

import com.inklusport.suscripciones.dto.PagoCheckoutResponse;
import com.inklusport.suscripciones.dto.PagoEventoResponse;
import com.inklusport.suscripciones.entity.ComprobantePago;
import com.inklusport.suscripciones.entity.ConfiguracionEventoPago;
import com.inklusport.suscripciones.entity.PagoEvento;
import com.inklusport.suscripciones.enums.EstadoPago;
import com.inklusport.suscripciones.exception.EventoNoConfiguradoComoPagoException;
import com.inklusport.suscripciones.exception.InscripcionDuplicadaException;
import com.inklusport.suscripciones.exception.PagoNotFoundException;
import com.inklusport.suscripciones.mercadopago.PaymentPreferenceResult;
import com.inklusport.suscripciones.mercadopago.PaymentStatusResult;
import com.inklusport.suscripciones.repository.ComprobantePagoRepository;
import com.inklusport.suscripciones.repository.PagoEventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** RF57, RF68, RF69, RF70: inscripcion y pago de eventos pagos, historial y comprobante. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagoEventoService {

    public static final String PREFIJO_REFERENCIA = "PE-";

    private final PagoEventoRepository pagoEventoRepository;
    private final ComprobantePagoRepository comprobantePagoRepository;
    private final ConfiguracionEventoPagoService configuracionEventoPagoService;
    private final PaymentGatewayClient paymentGatewayClient;
    private final ComprobanteService comprobanteService;
    private final EmailService emailService;

    @Transactional
    public PagoCheckoutResponse inscribirse(String usuarioId, String eventoId) {
        ConfiguracionEventoPago config = configuracionEventoPagoService.obtenerEntidadPorEvento(eventoId);
        if (!Boolean.TRUE.equals(config.getEsPago())) {
            throw new EventoNoConfiguradoComoPagoException(eventoId);
        }

        pagoEventoRepository.findByUsuarioIdAndEventoIdAndEstado(usuarioId, eventoId, EstadoPago.APROBADO)
                .ifPresent(p -> { throw new InscripcionDuplicadaException(usuarioId, eventoId); });

        PagoEvento pago = new PagoEvento();
        pago.setUsuarioId(usuarioId);
        pago.setEventoId(eventoId);
        pago.setMonto(config.getValorInscripcion());
        pago.setEstado(EstadoPago.PENDIENTE);
        pago = pagoEventoRepository.save(pago);

        String referencia = PREFIJO_REFERENCIA + pago.getId();
        pago.setReferenciaTransaccion(referencia);
        pago = pagoEventoRepository.save(pago);

        PaymentPreferenceResult preferencia = paymentGatewayClient.crearPreferencia(
                "Inscripcion a evento InkluSport", config.getValorInscripcion(), referencia);

        return PagoCheckoutResponse.builder()
                .pagoId(pago.getId())
                .monto(pago.getMonto())
                .estado(pago.getEstado())
                .referenciaTransaccion(referencia)
                .checkoutUrl(preferencia.getCheckoutUrl())
                .build();
    }

    /** Invocado por el webhook de Mercado Pago con el estado real ya consultado contra la API. */
    @Transactional
    public void confirmarPago(PaymentStatusResult status) {
        PagoEvento pago = pagoEventoRepository.findByReferenciaTransaccion(status.getReferenciaExterna())
                .orElseThrow(() -> new PagoNotFoundException(
                        "No se encontro el pago de evento con referencia: " + status.getReferenciaExterna()));

        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            log.info("Pago de evento {} ya estaba en estado {}, se ignora notificacion duplicada",
                    pago.getId(), pago.getEstado());
            return;
        }

        pago.setEstado(status.getEstado());
        pago.setMetodoPago(status.getMetodoPago());
        pagoEventoRepository.save(pago);

        if (status.getEstado() == EstadoPago.APROBADO) {
            // El pago ya quedo APROBADO: un fallo al generar el comprobante (RF69) o al enviar el
            // correo se registra pero NO revierte el cobro confirmado por Mercado Pago.
            try {
                ComprobantePago comprobante = comprobanteService.generarComprobanteEvento(
                        pago, "Inscripcion a evento " + pago.getEventoId());
                emailService.enviarComprobantePago(pago.getUsuarioId(), comprobante.getNumeroComprobante(),
                        "Inscripcion a evento", pago.getMonto(), comprobanteService.obtenerArchivo(comprobante));
            } catch (Exception e) {
                log.error("Pago de evento {} aprobado, pero fallo la emision del comprobante: {}",
                        pago.getId(), e.getMessage(), e);
            }
        } else {
            log.info("Pago de evento {} finalizo en estado {}", pago.getId(), status.getEstado());
        }
    }

    @Transactional(readOnly = true)
    public List<PagoEventoResponse> historialUsuario(String usuarioId) {
        return pagoEventoRepository.findByUsuarioIdOrderByFechaPagoDesc(usuarioId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.io.File obtenerComprobante(Long pagoId, String requesterId, boolean esAdmin) {
        PagoEvento pago = pagoEventoRepository.findById(pagoId)
                .orElseThrow(() -> new PagoNotFoundException("No se encontro el pago de evento con id: " + pagoId));
        if (!esAdmin && !pago.getUsuarioId().equals(requesterId)) {
            throw new AccessDeniedException("No tienes acceso a este comprobante");
        }
        ComprobantePago comprobante = comprobantePagoRepository.findByPagoEventoId(pagoId)
                .orElseThrow(() -> new PagoNotFoundException("El pago " + pagoId + " aun no tiene comprobante generado"));
        java.io.File archivo = comprobanteService.obtenerArchivo(comprobante);
        if (archivo == null || !archivo.exists()) {
            throw new PagoNotFoundException("El comprobante del pago " + pagoId + " no tiene un PDF disponible");
        }
        return archivo;
    }

    private PagoEventoResponse toResponse(PagoEvento pago) {
        var comprobante = comprobantePagoRepository.findByPagoEventoId(pago.getId()).orElse(null);
        return PagoEventoResponse.builder()
                .id(pago.getId())
                .usuarioId(pago.getUsuarioId())
                .eventoId(pago.getEventoId())
                .monto(pago.getMonto())
                .metodoPago(pago.getMetodoPago())
                .referenciaTransaccion(pago.getReferenciaTransaccion())
                .estado(pago.getEstado())
                .fechaPago(pago.getFechaPago())
                .comprobanteId(comprobante != null ? comprobante.getId() : null)
                .numeroComprobante(comprobante != null ? comprobante.getNumeroComprobante() : null)
                .build();
    }
}
