package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.PagoCheckoutResponse;
import com.inklusport.subscriptions.dto.PagoEstadoResponse;
import com.inklusport.subscriptions.dto.PagoSuscripcionResponse;
import com.inklusport.subscriptions.dto.PagoTarjetaRequest;
import com.inklusport.subscriptions.entity.HistorialSuscripcion;
import com.inklusport.subscriptions.entity.PagoSuscripcion;
import com.inklusport.subscriptions.entity.Plan;
import com.inklusport.subscriptions.entity.Suscripcion;
import com.inklusport.subscriptions.entity.TransaccionPasarela;
import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import com.inklusport.subscriptions.enums.OrigenSuscripcion;
import com.inklusport.subscriptions.enums.TipoMovimiento;
import com.inklusport.subscriptions.enums.TipoPagoSuscripcion;
import com.inklusport.subscriptions.exception.PagoNotFoundException;
import com.inklusport.subscriptions.exception.PlanNotFoundException;
import com.inklusport.subscriptions.mercadopago.PaymentStatusResult;
import com.inklusport.subscriptions.repository.ComprobantePagoRepository;
import com.inklusport.subscriptions.repository.HistorialSuscripcionRepository;
import com.inklusport.subscriptions.repository.PagoSuscripcionRepository;
import com.inklusport.subscriptions.repository.PlanRepository;
import com.inklusport.subscriptions.repository.SuscripcionRepository;
import com.inklusport.subscriptions.repository.TransaccionPasarelaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de pagos y activación de suscripciones.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagoSuscripcionService {

    public static final String PREFIJO_REFERENCIA = "PS-";

    private final PagoSuscripcionRepository pagoSuscripcionRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final HistorialSuscripcionRepository historialSuscripcionRepository;
    private final PlanRepository planRepository;
    private final ComprobantePagoRepository comprobantePagoRepository;
    private final TransaccionPasarelaRepository transaccionPasarelaRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final ComprobanteService comprobanteService;
    private final EmailService emailService;
    private final OrganizerIdentityService organizerIdentityService;

    /**
     * Inicia el cobro de una suscripción o la activa si el plan es gratuito.
     *
     * @param suscripcion suscripción a cobrar
     * @param planAplicar plan que se va a aplicar
     * @return datos de checkout del pago
     */
    @Transactional
    public PagoCheckoutResponse iniciarPago(Suscripcion suscripcion, Plan planAplicar) {
        if (planAplicar.getPrecio().compareTo(BigDecimal.ZERO) == 0) {
            activarSuscripcion(suscripcion, planAplicar);
            return PagoCheckoutResponse.builder()
                    .pagoId(null)
                    .monto(BigDecimal.ZERO)
                    .estado(EstadoPago.APROBADO)
                    .referenciaTransaccion(null)
                    .checkoutUrl(null)
                    .build();
        }

        TipoPagoSuscripcion tipo = resolverTipo(suscripcion, planAplicar);
        PagoSuscripcion pago = new PagoSuscripcion();
        pago.setSuscripcion(suscripcion);
        pago.setMonto(planAplicar.getPrecio());
        pago.setMoneda(planAplicar.getMoneda() != null ? planAplicar.getMoneda() : "COP");
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setTipo(tipo);
        pago = pagoSuscripcionRepository.save(pago);

        String referencia = PREFIJO_REFERENCIA + pago.getId() + "-" + planAplicar.getId();
        pago.setReferenciaTransaccion(referencia);
        pagoSuscripcionRepository.save(pago);

        // Checkout propio (SDK JS de Mercado Pago): el cobro ocurre en
        // pagarConTarjeta() cuando el usuario completa el formulario embebido.
        return PagoCheckoutResponse.builder()
                .pagoId(pago.getId())
                .monto(pago.getMonto())
                .estado(pago.getEstado())
                .referenciaTransaccion(referencia)
                .checkoutUrl(null)
                .build();
    }

    /**
     * Cobra un pago PENDIENTE ya creado por {@link #iniciarPago} con el token de tarjeta
     * generado por el formulario propio (SDK JS de Mercado Pago). Reutiliza
     * {@link #confirmarPago} para activar la suscripcion y emitir el comprobante, igual
     * que si la confirmacion viniera del webhook.
     */
    @Transactional
    public PagoEstadoResponse pagarConTarjeta(String principal, String referencia, PagoTarjetaRequest datos,
                                               boolean esAdmin) {
        PagoSuscripcion pago = pagoSuscripcionRepository.findByReferenciaTransaccion(referencia)
                .orElseThrow(() -> new PagoNotFoundException(
                        "No se encontro el pago de suscripcion con referencia: " + referencia));

        String organizadorId = organizerIdentityService.resolveUserId(principal);
        if (!esAdmin && !pago.getSuscripcion().getOrganizadorId().equals(organizadorId)) {
            throw new AccessDeniedException("No tienes acceso a este pago");
        }

        if (pago.getEstado() == EstadoPago.PENDIENTE) {
            Long planId = extraerPlanId(referencia);
            Plan plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
            String payerEmail = organizerIdentityService.resolveEmail(principal);

            PaymentStatusResult status = paymentGatewayClient.procesarPago(
                    datos.getCardToken(), pago.getMonto(), referencia,
                    "Suscripcion InkluSport - " + plan.getNombre(), datos.getInstallments(),
                    datos.getPaymentMethodId(), payerEmail, datos.getDocType(), datos.getDocNumber());

            confirmarPago(status);
        }

        return estadoActual(principal, referencia, esAdmin);
    }

    /**
     * Confirma el estado de un pago de suscripción según la pasarela. Invocado por el
     * webhook de Mercado Pago con el estado real ya consultado contra la API, o por
     * {@link #pagarConTarjeta} tras procesar el cobro con tarjeta.
     *
     * @param status resultado consultado en la pasarela
     */
    @Transactional
    public void confirmarPago(PaymentStatusResult status) {
        PagoSuscripcion pago = pagoSuscripcionRepository.findByReferenciaTransaccion(status.getReferenciaExterna())
                .orElseThrow(() -> new PagoNotFoundException(
                        "No se encontro el pago de suscripcion con referencia: " + status.getReferenciaExterna()));

        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            log.info("Pago de suscripcion {} ya estaba {}, se ignora duplicado", pago.getId(), pago.getEstado());
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
        pagoSuscripcionRepository.save(pago);

        if (status.getEstado() == EstadoPago.APROBADO) {
            Long planId = extraerPlanId(status.getReferenciaExterna());
            Plan plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
            Suscripcion suscripcion = activarSuscripcion(pago.getSuscripcion(), plan);
            try {
                var comprobante = comprobanteService.generarComprobanteSuscripcion(
                        pago, "Suscripcion plan " + plan.getNombre());
                emailService.enviarComprobantePago(
                        organizerIdentityService.resolveEmail(suscripcion.getOrganizadorId()),
                        comprobante.getNumeroComprobante(),
                        "Suscripcion plan " + plan.getNombre(), pago.getMonto(),
                        comprobanteService.obtenerArchivo(comprobante));
            } catch (Exception e) {
                log.error("Pago {} aprobado pero fallo el comprobante: {}", pago.getId(), e.getMessage(), e);
            }
        } else if (status.getEstado() == EstadoPago.RECHAZADO || status.getEstado() == EstadoPago.CANCELADO) {
            Suscripcion suscripcion = pago.getSuscripcion();
            boolean primeraVez = historialSuscripcionRepository
                    .findBySuscripcionIdOrderByFechaMovimientoDesc(suscripcion.getId()).isEmpty();
            if (primeraVez) {
                suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
                suscripcionRepository.save(suscripcion);
            }
        }
    }

    /**
     * Activa la suscripción aplicando el plan y registra el movimiento en historial.
     *
     * @param suscripcion suscripción a activar
     * @param plan        plan a aplicar
     * @return suscripción actualizada
     */
    @Transactional
    public Suscripcion activarSuscripcion(Suscripcion suscripcion, Plan plan) {
        LocalDate hoy = LocalDate.now();
        boolean primeraVez = historialSuscripcionRepository
                .findBySuscripcionIdOrderByFechaMovimientoDesc(suscripcion.getId()).isEmpty();
        boolean cambioPlan = !primeraVez && !suscripcion.getPlan().getId().equals(plan.getId());
        Long planAnteriorId = suscripcion.getPlan() != null ? suscripcion.getPlan().getId() : null;
        LocalDate fechaFinAnterior = suscripcion.getFechaFin();

        LocalDate baseFecha = (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA
                && suscripcion.getFechaFin() != null && !suscripcion.getFechaFin().isBefore(hoy))
                ? suscripcion.getFechaFin() : hoy;

        if (primeraVez) {
            suscripcion.setFechaInicio(hoy);
        }
        suscripcion.aplicarTerminos(plan);
        suscripcion.setFechaFin(baseFecha.plusDays(plan.getDuracionDias()));
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setPeriodoInicio(hoy.withDayOfMonth(1));
        if (cambioPlan || primeraVez) {
            suscripcion.setEventosCreadosPeriodo(0);
        }
        if (!primeraVez && !cambioPlan) {
            suscripcion.setFechaUltimaRenovacion(hoy);
            suscripcion.setOrigen(OrigenSuscripcion.RENOVACION);
        } else if (cambioPlan) {
            suscripcion.setOrigen(OrigenSuscripcion.CAMBIO_PLAN);
        }
        suscripcion = suscripcionRepository.save(suscripcion);

        HistorialSuscripcion historial = new HistorialSuscripcion();
        historial.setSuscripcion(suscripcion);
        historial.setTipoMovimiento(primeraVez ? TipoMovimiento.CREACION
                : (cambioPlan ? TipoMovimiento.CAMBIO_PLAN : TipoMovimiento.RENOVACION));
        historial.setPlanAnteriorId(primeraVez ? null : planAnteriorId);
        historial.setPlanNuevoId(plan.getId());
        historial.setEstadoNuevo(EstadoSuscripcion.ACTIVA.name());
        historial.setFechaFinAnterior(fechaFinAnterior);
        historial.setFechaFinNueva(suscripcion.getFechaFin());
        historial.setMonto(plan.getPrecio());
        historialSuscripcionRepository.save(historial);
        return suscripcion;
    }

    /**
     * Estado actual del cobro de una suscripcion por su referencia externa, con
     * verificacion de propiedad (el organizador solo ve los suyos; un ADMIN, todos).
     * Solo lectura: la reconciliacion contra Mercado Pago la orquesta {@link PagoConsultaService}.
     */
    @Transactional(readOnly = true)
    public PagoEstadoResponse estadoActual(String principal, String referencia, boolean esAdmin) {
        PagoSuscripcion pago = pagoSuscripcionRepository.findByReferenciaTransaccion(referencia)
                .orElseThrow(() -> new PagoNotFoundException(
                        "No se encontro el pago de suscripcion con referencia: " + referencia));
        Suscripcion suscripcion = pago.getSuscripcion();
        String organizadorId = organizerIdentityService.resolveUserId(principal);
        if (!esAdmin && !suscripcion.getOrganizadorId().equals(organizadorId)) {
            throw new AccessDeniedException("No tienes acceso a este pago");
        }
        return PagoEstadoResponse.builder()
                .referencia(referencia)
                .tipo("SUSCRIPCION")
                .pagoId(pago.getId())
                .estado(pago.getEstado())
                .monto(pago.getMonto())
                .suscripcionActiva(suscripcion.getEstado() == EstadoSuscripcion.ACTIVA)
                .reconciliadoAhora(false)
                .build();
    }

    /**
     * Lista los pagos asociados a una suscripción.
     *
     * @param suscripcionId identificador de la suscripción
     * @return pagos ordenados por fecha descendente
     */
    @Transactional(readOnly = true)
    public List<PagoSuscripcionResponse> listarPorSuscripcion(Long suscripcionId) {
        return pagoSuscripcionRepository.findBySuscripcionIdOrderByFechaPagoDesc(suscripcionId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.io.File obtenerComprobante(Long pagoId, String requesterId, boolean esAdmin) {
        PagoSuscripcion pago = pagoSuscripcionRepository.findById(pagoId)
                .orElseThrow(() -> new PagoNotFoundException("No se encontro el pago de suscripcion con id: " + pagoId));
        if (!esAdmin && !pago.getSuscripcion().getOrganizadorId().equals(requesterId)) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes acceso a este comprobante");
        }
        var comprobante = comprobantePagoRepository.findByPagoSuscripcionId(pagoId)
                .orElseThrow(() -> new PagoNotFoundException("El pago " + pagoId + " aun no tiene comprobante"));
        java.io.File archivo = comprobanteService.obtenerArchivo(comprobante);
        if (archivo == null || !archivo.exists()) {
            throw new PagoNotFoundException("El comprobante del pago " + pagoId + " no tiene un PDF disponible");
        }
        return archivo;
    }

    /**
     * Determina si el pago es de alta, cambio de plan o renovación.
     *
     * @param suscripcion suscripción involucrada
     * @param plan        plan a cobrar
     * @return tipo de pago
     */
    private TipoPagoSuscripcion resolverTipo(Suscripcion suscripcion, Plan plan) {
        boolean primeraVez = historialSuscripcionRepository
                .findBySuscripcionIdOrderByFechaMovimientoDesc(suscripcion.getId()).isEmpty();
        if (primeraVez) {
            return TipoPagoSuscripcion.NUEVA;
        }
        if (!suscripcion.getPlan().getId().equals(plan.getId())) {
            return TipoPagoSuscripcion.CAMBIO_PLAN;
        }
        return TipoPagoSuscripcion.RENOVACION;
    }

    /**
     * Extrae el identificador de plan embebido en la referencia.
     *
     * @param referencia referencia externa del pago
     * @return identificador del plan
     */
    private Long extraerPlanId(String referencia) {
        String[] partes = referencia.split("-");
        return Long.parseLong(partes[partes.length - 1]);
    }

    /**
     * Convierte el pago de suscripción a DTO de respuesta.
     *
     * @param pago pago persistido
     * @return DTO de respuesta
     */
    private PagoSuscripcionResponse toResponse(PagoSuscripcion pago) {
        var comprobante = comprobantePagoRepository.findByPagoSuscripcionId(pago.getId()).orElse(null);
        return PagoSuscripcionResponse.builder()
                .id(pago.getId())
                .suscripcionId(pago.getSuscripcion().getId())
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
