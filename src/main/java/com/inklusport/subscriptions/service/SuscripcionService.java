package com.inklusport.suscripciones.service;

import com.inklusport.suscripciones.dto.*;
import com.inklusport.suscripciones.entity.HistorialSuscripcion;
import com.inklusport.suscripciones.entity.Plan;
import com.inklusport.suscripciones.entity.Suscripcion;
import com.inklusport.suscripciones.enums.EstadoSuscripcion;
import com.inklusport.suscripciones.exception.PlanInactivoException;
import com.inklusport.suscripciones.exception.SuscripcionInactivaException;
import com.inklusport.suscripciones.exception.SuscripcionNotFoundException;
import com.inklusport.suscripciones.exception.LimiteEventosExcedidoException;
import com.inklusport.suscripciones.repository.HistorialSuscripcionRepository;
import com.inklusport.suscripciones.repository.PlanRepository;
import com.inklusport.suscripciones.repository.SuscripcionRepository;
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
 * RF58-RF61, RF63, RF66: ciclo de vida de la suscripcion de un organizador. La
 * orquestacion del cobro (pasarela, activacion al aprobar) vive en PagoSuscripcionService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final HistorialSuscripcionRepository historialSuscripcionRepository;
    private final PlanRepository planRepository;
    private final PlanService planService;
    private final PagoSuscripcionService pagoSuscripcionService;

    @Transactional
    public PagoCheckoutResponse crearSolicitud(String organizadorId, CrearSuscripcionRequest request) {
        Plan plan = planService.obtenerEntidad(request.getPlanId());
        if (!Boolean.TRUE.equals(plan.getActivo())) {
            throw new PlanInactivoException(plan.getId());
        }

        suscripcionRepository.findFirstByOrganizadorIdOrderByFechaCreacionDesc(organizadorId).ifPresent(existente -> {
            if (existente.getEstado() == EstadoSuscripcion.ACTIVA || existente.getEstado() == EstadoSuscripcion.SUSPENDIDA) {
                throw new SuscripcionInactivaException(
                        "Ya existe una suscripcion en estado " + existente.getEstado() +
                                "; usa la renovacion o espera a que finalice su ciclo actual.");
            }
        });

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setOrganizadorId(organizadorId);
        suscripcion.setPlan(plan);
        suscripcion.setFechaInicio(LocalDate.now());
        suscripcion.setFechaFin(LocalDate.now());
        suscripcion.setEstado(EstadoSuscripcion.SUSPENDIDA);
        suscripcion.setEventosCreadosMes(0);
        suscripcion.setRenovacionAutomatica(Boolean.TRUE.equals(request.getRenovacionAutomatica()));
        suscripcion = suscripcionRepository.save(suscripcion);

        return pagoSuscripcionService.iniciarPago(suscripcion, plan);
    }

    @Transactional
    public PagoCheckoutResponse renovar(String organizadorId, Long suscripcionId, RenovarSuscripcionRequest request) {
        Suscripcion suscripcion = obtenerPropia(organizadorId, suscripcionId);
        if (suscripcion.getEstado() == EstadoSuscripcion.CANCELADA) {
            throw new SuscripcionInactivaException("No se puede renovar una suscripcion cancelada");
        }

        Plan plan = request.getPlanId() != null ? planService.obtenerEntidad(request.getPlanId()) : suscripcion.getPlan();
        if (!Boolean.TRUE.equals(plan.getActivo())) {
            throw new PlanInactivoException(plan.getId());
        }

        return pagoSuscripcionService.iniciarPago(suscripcion, plan);
    }

    @Transactional(readOnly = true)
    public SuscripcionResponse obtenerActual(String organizadorId) {
        Suscripcion suscripcion = suscripcionRepository.findFirstByOrganizadorIdOrderByFechaCreacionDesc(organizadorId)
                .orElseThrow(() -> new SuscripcionNotFoundException("El organizador aun no tiene ninguna suscripcion"));
        return toResponse(suscripcion);
    }

    @Transactional(readOnly = true)
    public List<SuscripcionResponse> listarPropias(String organizadorId) {
        return suscripcionRepository.findByOrganizadorIdOrderByFechaCreacionDesc(organizadorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HistorialSuscripcionResponse> historialPropio(String organizadorId, Long suscripcionId) {
        obtenerPropia(organizadorId, suscripcionId);
        return historialSuscripcionRepository.findBySuscripcionIdOrderByFechaMovimientoDesc(suscripcionId).stream()
                .map(this::toHistorialResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HistorialSuscripcionResponse> historialPorOrganizador(String organizadorId) {
        return historialSuscripcionRepository.findBySuscripcion_OrganizadorIdOrderByFechaMovimientoDesc(organizadorId).stream()
                .map(this::toHistorialResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SuscripcionResponse cambiarEstado(Long suscripcionId, EstadoSuscripcion nuevoEstado) {
        Suscripcion suscripcion = obtenerEntidad(suscripcionId);
        suscripcion.setEstado(nuevoEstado);
        suscripcion = suscripcionRepository.save(suscripcion);
        log.info("Suscripcion {} cambio de estado a {}", suscripcionId, nuevoEstado);
        return toResponse(suscripcion);
    }

    @Transactional(readOnly = true)
    public PuedeCrearEventoResponse puedeCrearEvento(String organizadorId) {
        return suscripcionRepository
                .findFirstByOrganizadorIdAndEstadoOrderByFechaCreacionDesc(organizadorId, EstadoSuscripcion.ACTIVA)
                .map(s -> PuedeCrearEventoResponse.builder()
                        .puedeCrear(s.getEventosCreadosMes() < s.getPlan().getLimiteEventosMes())
                        .eventosCreadosMes(s.getEventosCreadosMes())
                        .limiteEventosMes(s.getPlan().getLimiteEventosMes())
                        .planNombre(s.getPlan().getNombre())
                        .build())
                .orElseGet(() -> PuedeCrearEventoResponse.builder()
                        .puedeCrear(false)
                        .eventosCreadosMes(0)
                        .limiteEventosMes(0)
                        .planNombre(null)
                        .build());
    }

    @Transactional
    public void registrarEventoCreado(String organizadorId) {
        Suscripcion suscripcion = suscripcionRepository
                .findFirstByOrganizadorIdAndEstadoOrderByFechaCreacionDesc(organizadorId, EstadoSuscripcion.ACTIVA)
                .orElseThrow(() -> new SuscripcionNotFoundException(
                        "El organizador " + organizadorId + " no tiene una suscripcion activa"));

        if (suscripcion.getEventosCreadosMes() >= suscripcion.getPlan().getLimiteEventosMes()) {
            throw new LimiteEventosExcedidoException(organizadorId, suscripcion.getPlan().getLimiteEventosMes());
        }
        suscripcionRepository.incrementarEventosCreados(suscripcion.getId());
    }

    /** RF66: idempotente, si el organizador ya tiene alguna suscripcion no la reemplaza. */
    @Transactional
    public SuscripcionResponse asignarPlanGratuito(String organizadorId) {
        var existente = suscripcionRepository.findFirstByOrganizadorIdOrderByFechaCreacionDesc(organizadorId);
        if (existente.isPresent()) {
            return toResponse(existente.get());
        }

        Plan planGratuito = planRepository.findByActivoTrue().stream()
                .filter(p -> p.getPrecio().compareTo(BigDecimal.ZERO) == 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay un plan gratuito activo configurado; no se puede asignar el plan inicial"));

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setOrganizadorId(organizadorId);
        suscripcion.setPlan(planGratuito);
        suscripcion.setFechaInicio(LocalDate.now());
        suscripcion.setFechaFin(LocalDate.now());
        suscripcion.setEstado(EstadoSuscripcion.SUSPENDIDA);
        suscripcion.setEventosCreadosMes(0);
        suscripcion.setRenovacionAutomatica(false);
        suscripcion = suscripcionRepository.save(suscripcion);

        pagoSuscripcionService.iniciarPago(suscripcion, planGratuito);
        log.info("Plan gratuito asignado automaticamente al organizador {}", organizadorId);
        return toResponse(suscripcion);
    }

    @Transactional(readOnly = true)
    public Suscripcion obtenerEntidad(Long suscripcionId) {
        return suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new SuscripcionNotFoundException(suscripcionId));
    }

    /** Publico para que los controladores de pagos/comprobantes tambien puedan validar la propiedad. */
    @Transactional(readOnly = true)
    public Suscripcion obtenerPropia(String organizadorId, Long suscripcionId) {
        Suscripcion suscripcion = obtenerEntidad(suscripcionId);
        if (!suscripcion.getOrganizadorId().equals(organizadorId)) {
            throw new AccessDeniedException("No tienes acceso a esta suscripcion");
        }
        return suscripcion;
    }

    private SuscripcionResponse toResponse(Suscripcion s) {
        return SuscripcionResponse.builder()
                .id(s.getId())
                .organizadorId(s.getOrganizadorId())
                .planId(s.getPlan().getId())
                .planNombre(s.getPlan().getNombre())
                .fechaInicio(s.getFechaInicio())
                .fechaFin(s.getFechaFin())
                .estado(s.getEstado())
                .eventosCreadosMes(s.getEventosCreadosMes())
                .limiteEventosMes(s.getPlan().getLimiteEventosMes())
                .renovacionAutomatica(s.getRenovacionAutomatica())
                .fechaCreacion(s.getFechaCreacion())
                .build();
    }

    private HistorialSuscripcionResponse toHistorialResponse(HistorialSuscripcion h) {
        return HistorialSuscripcionResponse.builder()
                .id(h.getId())
                .suscripcionId(h.getSuscripcion().getId())
                .tipoMovimiento(h.getTipoMovimiento())
                .planAnteriorId(h.getPlanAnteriorId())
                .planAnteriorNombre(nombrePlan(h.getPlanAnteriorId()))
                .planNuevoId(h.getPlanNuevoId())
                .planNuevoNombre(nombrePlan(h.getPlanNuevoId()))
                .fechaMovimiento(h.getFechaMovimiento())
                .build();
    }

    private String nombrePlan(Long planId) {
        if (planId == null) {
            return null;
        }
        return planRepository.findById(planId).map(Plan::getNombre).orElse(null);
    }
}
