package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.client.SportsServiceClient;
import com.inklusport.subscriptions.dto.PagoCheckoutResponse;
import com.inklusport.subscriptions.dto.PagoEstadoResponse;
import com.inklusport.subscriptions.dto.PagoEventoResponse;
import com.inklusport.subscriptions.dto.PagoTarjetaRequest;
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
import com.inklusport.subscriptions.mercadopago.PaymentStatusResult;
import com.inklusport.subscriptions.repository.ComprobantePagoRepository;
import com.inklusport.subscriptions.repository.PagoEventoRepository;
import com.inklusport.subscriptions.repository.TransaccionPasarelaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de pagos e inscripciones a eventos (RF57).
 * Checkout de inscripción cuando el evento tiene configuración {@code esPago};
 * el cobro aplica también a la primera inscripción del atleta.
 */
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
    private final SportsServiceClient sportsServiceClient;
    private final OrganizerIdentityService organizerIdentityService;

    @Value("${app.payment.mode:mock}")
    private String paymentMode;

    /**
     * Inicia la inscripción de pago a un evento (RF57 / RF70).
     * Solo crea el pago PENDIENTE; el cobro ocurre en el checkout propio de InkluSport
     * ({@link #pagarConTarjeta}), igual que las suscripciones — sin redirect a Checkout Pro.
     */
    public PagoCheckoutResponse inscribirse(String usuarioId, String eventoId) {
        ConfiguracionEventoPago config = configuracionEventoPagoService.obtenerEntidadPorEvento(eventoId);
        if (!Boolean.TRUE.equals(config.getEsPago())) {
            throw new EventoNoConfiguradoComoPagoException(eventoId);
        }
        pagoEventoRepository
                .findByUsuarioIdAndEventoIdAndEstadoOrderByIdDesc(usuarioId, eventoId, EstadoPago.APROBADO)
                .stream()
                .findFirst()
                .ifPresent(p -> { throw new InscripcionDuplicadaException(usuarioId, eventoId); });

        // Reutilizar el PENDIENTE más reciente; si hay duplicados de intentos fallidos, se cancelan.
        List<PagoEvento> pendientes = pagoEventoRepository
                .findByUsuarioIdAndEventoIdAndEstadoOrderByIdDesc(usuarioId, eventoId, EstadoPago.PENDIENTE);
        if (!pendientes.isEmpty()) {
            PagoEvento pago = pendientes.get(0);
            for (int i = 1; i < pendientes.size(); i++) {
                PagoEvento dup = pendientes.get(i);
                dup.setEstado(EstadoPago.CANCELADO);
                pagoEventoRepository.save(dup);
                log.info("Pago evento duplicado {} cancelado (queda {})", dup.getId(), pago.getId());
            }
            return PagoCheckoutResponse.builder()
                    .pagoId(pago.getId())
                    .monto(pago.getMonto())
                    .estado(pago.getEstado())
                    .referenciaTransaccion(pago.getReferenciaTransaccion())
                    .checkoutUrl(null)
                    .build();
        }

        BigDecimal monto = config.getValorInscripcion();
        BigDecimal porcentaje = config.getPorcentajeComision() != null ? config.getPorcentajeComision() : BigDecimal.ZERO;
        BigDecimal comision = monto.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        PagoEvento pago = new PagoEvento();
        pago.setUsuarioId(usuarioId);
        pago.setEventoId(eventoId);
        pago.setOrganizadorId(config.getOrganizadorId());
        pago.setNombreEvento(sportsServiceClient.obtenerNombreEvento(eventoId));
        pago.setMonto(monto);
        pago.setMoneda(config.getMoneda() != null ? config.getMoneda() : "COP");
        pago.setPorcentajeComision(porcentaje);
        pago.setComisionPlataforma(comision);
        pago.setMontoNetoOrganizador(monto.subtract(comision));
        pago.setEstado(EstadoPago.PENDIENTE);
        pago = pagoEventoRepository.save(pago);

        String referencia = PREFIJO_REFERENCIA + pago.getId();
        pago.setReferenciaTransaccion(referencia);
        pago = pagoEventoRepository.save(pago);

        return PagoCheckoutResponse.builder()
                .pagoId(pago.getId())
                .monto(pago.getMonto())
                .estado(pago.getEstado())
                .referenciaTransaccion(referencia)
                .checkoutUrl(null)
                .build();
    }

    /**
     * Cobra un pago de inscripción PENDIENTE con el token de tarjeta del formulario
     * embebido en InkluSport (RF70). Confirma inscripción en sports si MP aprueba.
     */
    public PagoEstadoResponse pagarConTarjeta(String principal, String referencia, PagoTarjetaRequest datos,
                                               boolean esAdmin) {
        PagoEvento pago = pagoEventoRepository.findByReferenciaTransaccion(referencia)
                .orElseThrow(() -> new PagoNotFoundException(
                        "No se encontro el pago de evento con referencia: " + referencia));

        String usuarioId = organizerIdentityService.resolveUserId(principal);
        if (!esAdmin && !pago.getUsuarioId().equals(usuarioId)) {
            throw new AccessDeniedException("No tienes acceso a este pago");
        }

        if (pago.getEstado() == EstadoPago.PENDIENTE) {
            String payerEmail = organizerIdentityService.resolveEmail(principal);
            String titulo = "Inscripcion InkluSport - "
                    + (pago.getNombreEvento() != null ? pago.getNombreEvento() : pago.getEventoId());

            PaymentStatusResult status = paymentGatewayClient.procesarPago(
                    datos.getCardToken(), pago.getMonto(), referencia, titulo,
                    datos.getInstallments(), datos.getPaymentMethodId(), payerEmail,
                    datos.getDocType(), datos.getDocNumber());

            // Auditoría de pasarela: no debe impedir confirmar la inscripción si MP ya cobró.
            try {
                TransaccionPasarela tx = new TransaccionPasarela();
                tx.setPasarela("mock".equalsIgnoreCase(paymentMode) ? Pasarela.MOCK : Pasarela.MERCADOPAGO);
                tx.setTipo(TipoTransaccion.INSCRIPCION_EVENTO);
                tx.setReferenciaExterna(referencia);
                tx.setMoneda(pago.getMoneda());
                tx.setMonto(pago.getMonto());
                tx.setPagoExternoId(status.getPaymentIdExterno());
                tx.setEstadoPasarela(status.getEstadoPasarela());
                tx.setDetalleEstado(status.getDetalleEstado());
                tx.setMetodoPago(status.getMetodoPago());
                tx.setTipoPago(status.getTipoPago());
                tx = transaccionPasarelaRepository.save(tx);
                pago.setTransaccionId(tx.getId());
                pagoEventoRepository.save(pago);
            } catch (RuntimeException e) {
                log.error("Cobro PE {} OK en pasarela pero falló guardar transaccion_pasarela: {}",
                        referencia, e.getMessage(), e);
            }

            confirmarPago(status);
        }

        return estadoActual(principal, referencia, esAdmin);
    }

    /**
     * Confirma el estado de un pago de evento según la pasarela.
     *
     * @param status resultado consultado en la pasarela
     */
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
        if (pago.getTransaccionId() != null) {
            transaccionPasarelaRepository.findById(pago.getTransaccionId()).ifPresent(tx -> {
                if (status.getPaymentIdExterno() != null && !status.getPaymentIdExterno().isBlank()) {
                    tx.setPagoExternoId(status.getPaymentIdExterno());
                }
                tx.setEstadoPasarela(status.getEstadoPasarela());
                tx.setDetalleEstado(status.getDetalleEstado());
                tx.setMetodoPago(status.getMetodoPago());
                tx.setTipoPago(status.getTipoPago());
                transaccionPasarelaRepository.save(tx);
            });
        }
        pagoEventoRepository.save(pago);

        if (status.getEstado() == EstadoPago.APROBADO) {
            // ink-ms-sports guarda las inscripciones por email (no por UUID de
            // ink-ms-users), igual que registerToEvent(): hay que resolver antes de llamar.
            String emailUsuario = organizerIdentityService.resolveEmail(pago.getUsuarioId());
            sportsServiceClient.confirmarInscripcionPagada(emailUsuario, pago.getEventoId());
            try {
                ComprobantePago comprobante = comprobanteService.generarComprobanteEvento(
                        pago, "Inscripcion a evento " + (pago.getNombreEvento() != null
                                ? pago.getNombreEvento() : pago.getEventoId()));
                emailService.enviarComprobantePago(
                        organizerIdentityService.resolveEmail(pago.getUsuarioId()),
                        comprobante.getNumeroComprobante(),
                        "Inscripcion a evento", pago.getMonto(), comprobanteService.obtenerArchivo(comprobante));
            } catch (Exception e) {
                log.error("Pago evento {} aprobado pero fallo el comprobante: {}", pago.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Estado actual del cobro de una inscripcion a evento por su referencia externa,
     * con verificacion de propiedad (el usuario solo ve los suyos; un ADMIN, todos).
     * Solo lectura: la reconciliacion contra Mercado Pago la orquesta {@link PagoConsultaService}.
     */
    public PagoEstadoResponse estadoActual(String email, String referencia, boolean esAdmin) {
        PagoEvento pago = pagoEventoRepository.findByReferenciaTransaccion(referencia)
                .orElseThrow(() -> new PagoNotFoundException(
                        "No se encontro el pago de evento con referencia: " + referencia));
        if (!esAdmin && !pago.getUsuarioId().equals(organizerIdentityService.resolveUserId(email))) {
            throw new AccessDeniedException("No tienes acceso a este pago");
        }
        return PagoEstadoResponse.builder()
                .referencia(referencia)
                .tipo("EVENTO")
                .pagoId(pago.getId())
                .estado(pago.getEstado())
                .monto(pago.getMonto())
                .suscripcionActiva(null)
                .reconciliadoAhora(false)
                .build();
    }

    /**
     * Lista el historial de pagos de evento de un usuario.
     *
     * @param usuarioId identificador del usuario
     * @return pagos ordenados por fecha descendente
     */
    public List<PagoEventoResponse> historialUsuario(String usuarioId) {
        return pagoEventoRepository.findByUsuarioIdOrderByFechaPagoDesc(usuarioId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * RF66: ingresos por inscripción que recibió el organizador (denormalizado en organizador_id).
     *
     * @param organizadorId identificador del organizador
     * @return pagos de sus eventos, más recientes primero
     */
    public List<PagoEventoResponse> historialOrganizador(String organizadorId) {
        return pagoEventoRepository.findByOrganizadorIdOrderByFechaPagoDesc(organizadorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el PDF del comprobante de un pago de evento.
     *
     * @param pagoId      identificador del pago
     * @param requesterId usuario que solicita el archivo
     * @param esAdmin     {@code true} si el solicitante es administrador
     * @return archivo PDF del comprobante
     */
    public java.io.File obtenerComprobante(Long pagoId, String requesterId, boolean esAdmin) {
        PagoEvento pago = pagoEventoRepository.findById(pagoId)
                .orElseThrow(() -> new PagoNotFoundException("No se encontro el pago de evento con id: " + pagoId));
        if (!esAdmin
                && !pago.getUsuarioId().equals(requesterId)
                && !pago.getOrganizadorId().equals(requesterId)) {
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

    /**
     * Convierte el pago de evento a DTO de respuesta.
     *
     * @param pago pago persistido
     * @return DTO de respuesta
     */
    private PagoEventoResponse toResponse(PagoEvento pago) {
        var comprobante = comprobantePagoRepository.findByPagoEventoId(pago.getId()).orElse(null);
        return PagoEventoResponse.builder()
                .id(pago.getId())
                .usuarioId(pago.getUsuarioId())
                .eventoId(pago.getEventoId())
                .organizadorId(pago.getOrganizadorId())
                .nombreEvento(pago.getNombreEvento())
                .monto(pago.getMonto())
                .moneda(pago.getMoneda())
                .metodoPago(pago.getMetodoPago())
                .referenciaTransaccion(pago.getReferenciaTransaccion())
                .estado(pago.getEstado())
                .fechaPago(pago.getFechaPago())
                .comprobanteId(comprobante != null ? comprobante.getId() : null)
                .numeroComprobante(comprobante != null ? comprobante.getNumeroComprobante() : null)
                .build();
    }
}
