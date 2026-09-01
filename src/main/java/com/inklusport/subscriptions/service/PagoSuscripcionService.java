package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.PagoCheckoutResponse;
import com.inklusport.subscriptions.dto.PagoSuscripcionResponse;
import com.inklusport.subscriptions.entity.HistorialSuscripcion;
import com.inklusport.subscriptions.entity.PagoSuscripcion;
import com.inklusport.subscriptions.entity.Plan;
import com.inklusport.subscriptions.entity.Suscripcion;
import com.inklusport.subscriptions.entity.TransaccionPasarela;
import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import com.inklusport.subscriptions.enums.OrigenSuscripcion;
import com.inklusport.subscriptions.enums.Pasarela;
import com.inklusport.subscriptions.enums.TipoMovimiento;
import com.inklusport.subscriptions.enums.TipoPagoSuscripcion;
import com.inklusport.subscriptions.enums.TipoTransaccion;
import com.inklusport.subscriptions.exception.PagoNotFoundException;
import com.inklusport.subscriptions.exception.PlanNotFoundException;
import com.inklusport.subscriptions.payment.PaymentPreferenceResult;
import com.inklusport.subscriptions.payment.PaymentStatusResult;
import com.inklusport.subscriptions.repository.ComprobantePagoRepository;
import com.inklusport.subscriptions.repository.HistorialSuscripcionRepository;
import com.inklusport.subscriptions.repository.PagoSuscripcionRepository;
import com.inklusport.subscriptions.repository.PlanRepository;
import com.inklusport.subscriptions.repository.SuscripcionRepository;
import com.inklusport.subscriptions.repository.TransaccionPasarelaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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

    @Value("${app.payment.mode:mock}")
    private String paymentMode;

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

        PaymentPreferenceResult preferencia = paymentGatewayClient.crearPreferencia(
                "Suscripcion InkluSport - " + planAplicar.getNombre(), planAplicar.getPrecio(), referencia);

        TransaccionPasarela tx = new TransaccionPasarela();
        tx.setPasarela(esMock() ? Pasarela.MOCK : Pasarela.MERCADOPAGO);
        tx.setTipo(TipoTransaccion.SUSCRIPCION_ORGANIZADOR);
        tx.setPreferenciaId(preferencia.getPreferenceId());
        tx.setReferenciaExterna(referencia);
        tx.setMoneda(pago.getMoneda());
        tx.setMonto(pago.getMonto());
        tx.setUrlCheckout(preferencia.getCheckoutUrl());
        tx.setEstadoPasarela("pending");
        tx = transaccionPasarelaRepository.save(tx);
        pago.setTransaccion(tx);
        pago = pagoSuscripcionRepository.save(pago);

        if (esMock()) {
            PaymentStatusResult mock = paymentGatewayClient.consultarPago(preferencia.getPreferenceId());
            mock.setReferenciaExterna(referencia);
            confirmarPago(mock);
            pago = pagoSuscripcionRepository.findById(pago.getId()).orElse(pago);
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
                emailService.enviarComprobantePago(suscripcion.getOrganizadorId(), comprobante.getNumeroComprobante(),
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

    @Transactional(readOnly = true)
    public List<PagoSuscripcionResponse> listarPorSuscripcion(Long suscripcionId) {
        return pagoSuscripcionRepository.findBySuscripcionIdOrderByFechaPagoDesc(suscripcionId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

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

    private boolean esMock() {
        return "mock".equalsIgnoreCase(paymentMode);
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
