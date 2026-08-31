package com.inklusport.suscripciones.service;

import com.inklusport.suscripciones.dto.PagoCheckoutResponse;
import com.inklusport.suscripciones.dto.PagoSuscripcionResponse;
import com.inklusport.suscripciones.entity.HistorialSuscripcion;
import com.inklusport.suscripciones.entity.PagoSuscripcion;
import com.inklusport.suscripciones.entity.Plan;
import com.inklusport.suscripciones.entity.Suscripcion;
import com.inklusport.suscripciones.enums.EstadoPago;
import com.inklusport.suscripciones.enums.EstadoSuscripcion;
import com.inklusport.suscripciones.enums.TipoMovimiento;
import com.inklusport.suscripciones.exception.PagoNotFoundException;
import com.inklusport.suscripciones.exception.PlanNotFoundException;
import com.inklusport.suscripciones.mercadopago.PaymentPreferenceResult;
import com.inklusport.suscripciones.mercadopago.PaymentStatusResult;
import com.inklusport.suscripciones.repository.ComprobantePagoRepository;
import com.inklusport.suscripciones.repository.HistorialSuscripcionRepository;
import com.inklusport.suscripciones.repository.PagoSuscripcionRepository;
import com.inklusport.suscripciones.repository.PlanRepository;
import com.inklusport.suscripciones.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RF58, RF61, RF69, RF70: orquesta el cobro de una suscripcion (nueva, renovacion o
 * cambio de plan) contra Mercado Pago y aplica la activacion cuando el pago es aprobado.
 *
 * Depende solo de repositorios (no de SuscripcionService) para evitar un ciclo de
 * dependencias, ya que SuscripcionService es quien invoca a este servicio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagoSuscripcionService {

    private final PagoSuscripcionRepository pagoSuscripcionRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final HistorialSuscripcionRepository historialSuscripcionRepository;
    private final PlanRepository planRepository;
    private final ComprobantePagoRepository comprobantePagoRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final ComprobanteService comprobanteService;
    private final EmailService emailService;

    public static final String PREFIJO_REFERENCIA = "PS-";

    /**
     * Si el plan aplicado es gratuito (precio 0) activa la suscripcion de inmediato,
     * sin pasar por la pasarela de pago. En caso contrario crea un pago PENDIENTE y
     * devuelve el link de checkout de Mercado Pago.
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

        PagoSuscripcion pago = new PagoSuscripcion();
        pago.setSuscripcion(suscripcion);
        pago.setMonto(planAplicar.getPrecio());
        pago.setEstado(EstadoPago.PENDIENTE);
        pago = pagoSuscripcionRepository.save(pago);

        String referencia = PREFIJO_REFERENCIA + pago.getId() + "-" + planAplicar.getId();
        pago.setReferenciaTransaccion(referencia);
        pago = pagoSuscripcionRepository.save(pago);

        PaymentPreferenceResult preferencia = paymentGatewayClient.crearPreferencia(
                "Suscripcion InkluSport - " + planAplicar.getNombre(), planAplicar.getPrecio(), referencia);

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
        PagoSuscripcion pago = pagoSuscripcionRepository.findByReferenciaTransaccion(status.getReferenciaExterna())
                .orElseThrow(() -> new PagoNotFoundException(
                        "No se encontro el pago de suscripcion con referencia: " + status.getReferenciaExterna()));

        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            log.info("Pago de suscripcion {} ya estaba en estado {}, se ignora notificacion duplicada",
                    pago.getId(), pago.getEstado());
            return;
        }

        pago.setEstado(status.getEstado());
        pago.setMetodoPago(status.getMetodoPago());
        pagoSuscripcionRepository.save(pago);

        if (status.getEstado() == EstadoPago.APROBADO) {
            Long planId = extraerPlanId(status.getReferenciaExterna());
            Plan plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

            Suscripcion suscripcion = activarSuscripcion(pago.getSuscripcion(), plan);

            // La suscripcion ya quedo activada y el pago APROBADO: si falla la generacion del
            // comprobante (RF69) o el envio del correo se registra el error, pero NO se revierte
            // el pago confirmado por Mercado Pago (el comprobante se puede regenerar despues).
            try {
                var comprobante = comprobanteService.generarComprobanteSuscripcion(
                        pago, "Suscripcion plan " + plan.getNombre());
                emailService.enviarComprobantePago(suscripcion.getOrganizadorId(), comprobante.getNumeroComprobante(),
                        "Suscripcion plan " + plan.getNombre(), pago.getMonto(),
                        comprobanteService.obtenerArchivo(comprobante));
            } catch (Exception e) {
                log.error("Pago de suscripcion {} aprobado y suscripcion activada, pero fallo la emision del comprobante: {}",
                        pago.getId(), e.getMessage(), e);
            }
        } else if (status.getEstado() == EstadoPago.RECHAZADO) {
            Suscripcion suscripcion = pago.getSuscripcion();
            boolean primeraVez = historialSuscripcionRepository
                    .findBySuscripcionIdOrderByFechaMovimientoDesc(suscripcion.getId()).isEmpty();
            if (primeraVez) {
                suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
                suscripcionRepository.save(suscripcion);
            }
            log.info("Pago de suscripcion {} rechazado", pago.getId());
        }
    }

    @Transactional
    public Suscripcion activarSuscripcion(Suscripcion suscripcion, Plan plan) {
        LocalDate hoy = LocalDate.now();
        boolean primeraVez = historialSuscripcionRepository
                .findBySuscripcionIdOrderByFechaMovimientoDesc(suscripcion.getId()).isEmpty();
        boolean cambioPlan = !primeraVez && !suscripcion.getPlan().getId().equals(plan.getId());
        Long planAnteriorId = suscripcion.getPlan() != null ? suscripcion.getPlan().getId() : null;

        LocalDate baseFecha = (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA
                && suscripcion.getFechaFin() != null && !suscripcion.getFechaFin().isBefore(hoy))
                ? suscripcion.getFechaFin() : hoy;

        if (primeraVez) {
            suscripcion.setFechaInicio(hoy);
        }
        suscripcion.setFechaFin(baseFecha.plusDays(plan.getDuracionDias()));
        suscripcion.setPlan(plan);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        if (cambioPlan) {
            suscripcion.setEventosCreadosMes(0);
        }
        suscripcion = suscripcionRepository.save(suscripcion);

        HistorialSuscripcion historial = new HistorialSuscripcion();
        historial.setSuscripcion(suscripcion);
        historial.setTipoMovimiento(primeraVez ? TipoMovimiento.CREACION
                : (cambioPlan ? TipoMovimiento.CAMBIO_PLAN : TipoMovimiento.RENOVACION));
        historial.setPlanAnteriorId(primeraVez ? null : planAnteriorId);
        historial.setPlanNuevoId(plan.getId());
        historialSuscripcionRepository.save(historial);

        return suscripcion;
    }

    @Transactional(readOnly = true)
    public List<PagoSuscripcionResponse> listarPorSuscripcion(Long suscripcionId) {
        return pagoSuscripcionRepository.findBySuscripcionIdOrderByFechaPagoDesc(suscripcionId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Long extraerPlanId(String referencia) {
        String[] partes = referencia.split("-");
        return Long.parseLong(partes[partes.length - 1]);
    }

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
