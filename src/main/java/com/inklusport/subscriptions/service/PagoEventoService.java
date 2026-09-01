package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.PagoCheckoutResponse;
import com.inklusport.subscriptions.dto.PagoEventoResponse;
import com.inklusport.subscriptions.entity.ComprobantePago;
import com.inklusport.subscriptions.entity.ConfiguracionEventoPago;
import com.inklusport.subscriptions.entity.PagoEvento;
import com.inklusport.subscriptions.entity.TransaccionPasarela;
import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.enums.Pasarela;
import com.inklusport.subscriptions.enums.TipoTransaccion;
import com.inklusport.subscriptions.exception.EventoNoConfiguradoComoPagoException;
import com.inklusport.subscriptions.exception.InscripcionDuplicadaException;
import com.inklusport.subscriptions.exception.PagoNotFoundException;
import com.inklusport.subscriptions.payment.PaymentPreferenceResult;
import com.inklusport.subscriptions.payment.PaymentStatusResult;
import com.inklusport.subscriptions.repository.ComprobantePagoRepository;
import com.inklusport.subscriptions.repository.PagoEventoRepository;
import com.inklusport.subscriptions.repository.TransaccionPasarelaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoEventoService {

    public static final String PREFIJO_REFERENCIA = "PE-";

    private final PagoEventoRepository pagoEventoRepository;
    private final ComprobantePagoRepository comprobantePagoRepository;
    private final TransaccionPasarelaRepository transaccionPasarelaRepository;
    private final ConfiguracionEventoPagoService configuracionEventoPagoService;
    private final PaymentGatewayClient paymentGatewayClient;
    private final ComprobanteService comprobanteService;
    private final EmailService emailService;

    @Value("${app.payment.mode:mock}")
    private String paymentMode;

    @Transactional
    public PagoCheckoutResponse inscribirse(String usuarioId, String eventoId) {
        ConfiguracionEventoPago config = configuracionEventoPagoService.obtenerEntidadPorEvento(eventoId);
        if (!Boolean.TRUE.equals(config.getEsPago())) {
            throw new EventoNoConfiguradoComoPagoException(eventoId);
        }
        pagoEventoRepository.findByUsuarioIdAndEventoIdAndEstado(usuarioId, eventoId, EstadoPago.APROBADO)
                .ifPresent(p -> { throw new InscripcionDuplicadaException(usuarioId, eventoId); });

        BigDecimal monto = config.getValorInscripcion();
        BigDecimal porcentaje = config.getPorcentajeComision() != null ? config.getPorcentajeComision() : BigDecimal.ZERO;
        BigDecimal comision = monto.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        PagoEvento pago = new PagoEvento();
        pago.setUsuarioId(usuarioId);
        pago.setEventoId(eventoId);
        pago.setOrganizadorId(config.getOrganizadorId());
        pago.setMonto(monto);
        pago.setMoneda(config.getMoneda() != null ? config.getMoneda() : "COP");
        pago.setPorcentajeComision(porcentaje);
        pago.setComisionPlataforma(comision);
        pago.setMontoNetoOrganizador(monto.subtract(comision));
        pago.setEstado(EstadoPago.PENDIENTE);
        pago = pagoEventoRepository.save(pago);

        String referencia = PREFIJO_REFERENCIA + pago.getId();
        pago.setReferenciaTransaccion(referencia);

        PaymentPreferenceResult preferencia = paymentGatewayClient.crearPreferencia(
                "Inscripcion a evento InkluSport", monto, referencia);

        TransaccionPasarela tx = new TransaccionPasarela();
        tx.setPasarela("mock".equalsIgnoreCase(paymentMode) ? Pasarela.MOCK : Pasarela.MERCADOPAGO);
        tx.setTipo(TipoTransaccion.INSCRIPCION_EVENTO);
        tx.setPreferenciaId(preferencia.getPreferenceId());
        tx.setReferenciaExterna(referencia);
        tx.setMoneda(pago.getMoneda());
        tx.setMonto(monto);
        tx.setUrlCheckout(preferencia.getCheckoutUrl());
        tx.setEstadoPasarela("pending");
        tx = transaccionPasarelaRepository.save(tx);
        pago.setTransaccion(tx);
        pago = pagoEventoRepository.save(pago);

        if ("mock".equalsIgnoreCase(paymentMode)) {
            PaymentStatusResult mock = paymentGatewayClient.consultarPago(preferencia.getPreferenceId());
            mock.setReferenciaExterna(referencia);
            confirmarPago(mock);
            pago = pagoEventoRepository.findById(pago.getId()).orElse(pago);
        }

        return PagoCheckoutResponse.builder()
                .pagoId(pago.getId())
                .monto(pago.getMonto())
                .estado(pago.getEstado())
                .referenciaTransaccion(referencia)
                .checkoutUrl(preferencia.getCheckoutUrl())
                .build();
    }

    @Transactional
    public void confirmarPago(PaymentStatusResult status) {
        PagoEvento pago = pagoEventoRepository.findByReferenciaTransaccion(status.getReferenciaExterna())
                .orElseThrow(() -> new PagoNotFoundException(
                        "No se encontro el pago de evento con referencia: " + status.getReferenciaExterna()));

        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            log.info("Pago de evento {} ya estaba {}, se ignora duplicado", pago.getId(), pago.getEstado());
            return;
        }

        pago.setEstado(status.getEstado());
        pago.setMetodoPago(status.getMetodoPago());
        if (pago.getTransaccion() != null) {
            TransaccionPasarela tx = pago.getTransaccion();
            tx.setPagoExternoId(status.getPaymentIdExterno());
            tx.setEstadoPasarela(status.getEstadoPasarela());
            tx.setDetalleEstado(status.getDetalleEstado());
            tx.setMetodoPago(status.getMetodoPago());
            tx.setTipoPago(status.getTipoPago());
            transaccionPasarelaRepository.save(tx);
        }
        pagoEventoRepository.save(pago);

        if (status.getEstado() == EstadoPago.APROBADO) {
            try {
                ComprobantePago comprobante = comprobanteService.generarComprobanteEvento(
                        pago, "Inscripcion a evento " + pago.getEventoId());
                emailService.enviarComprobantePago(pago.getUsuarioId(), comprobante.getNumeroComprobante(),
                        "Inscripcion a evento", pago.getMonto(), comprobanteService.obtenerArchivo(comprobante));
            } catch (Exception e) {
                log.error("Pago evento {} aprobado pero fallo el comprobante: {}", pago.getId(), e.getMessage(), e);
            }
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
                .orElseThrow(() -> new PagoNotFoundException("El pago " + pagoId + " aun no tiene comprobante"));
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
