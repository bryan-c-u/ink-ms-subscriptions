package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.*;
import com.inklusport.subscriptions.entity.HistorialSuscripcion;
import com.inklusport.subscriptions.entity.Plan;
import com.inklusport.subscriptions.entity.Suscripcion;
import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import com.inklusport.subscriptions.enums.OrigenSuscripcion;
import com.inklusport.subscriptions.enums.TipoMovimiento;
import com.inklusport.subscriptions.exception.LimiteEventosExcedidoException;
import com.inklusport.subscriptions.exception.PlanInactivoException;
import com.inklusport.subscriptions.exception.SuscripcionInactivaException;
import com.inklusport.subscriptions.exception.SuscripcionNotFoundException;
import com.inklusport.subscriptions.repository.HistorialSuscripcionRepository;
import com.inklusport.subscriptions.repository.PlanRepository;
import com.inklusport.subscriptions.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de ciclo de vida de suscripciones del organizador.
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
    private final OrganizerIdentityService organizerIdentityService;

    /**
     * Crea una solicitud de suscripción y arranca el cobro del plan.
     *
     * @param organizadorId identificador del organizador
     * @param request       plan y opciones de renovación
     * @return datos de checkout del pago
     */
    public PagoCheckoutResponse crearSolicitud(String organizadorId, CrearSuscripcionRequest request) {
        Plan plan = planService.obtenerEntidad(request.getPlanId());
        if (!Boolean.TRUE.equals(plan.getActivo())) {
            throw new PlanInactivoException(plan.getId());
        }
        suscripcionRepository.findFirstByOrganizadorIdOrderByFechaCreacionDesc(organizadorId).ifPresent(existente -> {
            if (existente.getEstado() == EstadoSuscripcion.ACTIVA || existente.getEstado() == EstadoSuscripcion.SUSPENDIDA) {
                throw new SuscripcionInactivaException(
                        "Ya existe una suscripcion en estado " + existente.getEstado());
            }
        });

        LocalDate hoy = LocalDate.now();
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setOrganizadorId(organizadorId);
        suscripcion.aplicarTerminos(plan);
        suscripcion.setFechaInicio(hoy);
        suscripcion.setFechaFin(hoy);
        suscripcion.setPeriodoInicio(hoy.withDayOfMonth(1));
        suscripcion.setEstado(EstadoSuscripcion.SUSPENDIDA);
        suscripcion.setEventosCreadosPeriodo(0);
        suscripcion.setOrigen(OrigenSuscripcion.COMPRA);
        suscripcion.setRenovacionAutomatica(Boolean.TRUE.equals(request.getRenovacionAutomatica()));
        suscripcion = suscripcionRepository.save(suscripcion);

        return pagoSuscripcionService.iniciarPago(suscripcion, plan);
    }

    /**
     * Renueva o cambia el plan de una suscripción propia.
     *
     * @param organizadorId identificador del organizador
     * @param suscripcionId identificador de la suscripción
     * @param request       plan destino, o el actual si no se indica
     * @return datos de checkout del pago
     */
    public PagoCheckoutResponse renovar(String organizadorId, Long suscripcionId, RenovarSuscripcionRequest request) {
        Suscripcion suscripcion = obtenerPropia(organizadorId, suscripcionId);
        if (suscripcion.getEstado() == EstadoSuscripcion.CANCELADA) {
            throw new SuscripcionInactivaException("No se puede renovar una suscripcion cancelada");
        }
        Long planDestinoId = request.getPlanId() != null ? request.getPlanId() : suscripcion.getPlanId();
        Plan plan = planService.obtenerEntidad(planDestinoId);
        if (!Boolean.TRUE.equals(plan.getActivo())) {
            throw new PlanInactivoException(plan.getId());
        }
        return pagoSuscripcionService.iniciarPago(suscripcion, plan);
    }

    /**
     * Obtiene la suscripción más reciente del organizador.
     * Si todavía no tiene ninguna (rol asignado antes de cablear RF64),
     * se le asigna el plan gratuito inicial.
     *
     * @param organizadorId identificador del organizador
     * @return suscripción actual
     */
    public SuscripcionResponse obtenerActual(String organizadorId) {
        return suscripcionRepository.findFirstByOrganizadorIdOrderByFechaCreacionDesc(organizadorId)
                .map(this::toResponse)
                .orElseGet(() -> asignarPlanGratuito(organizadorId));
    }

    /**
     * Lista las suscripciones del organizador.
     *
     * @param organizadorId identificador del organizador
     * @return suscripciones propias
     */
    public List<SuscripcionResponse> listarPropias(String organizadorId) {
        return suscripcionRepository.findByOrganizadorIdOrderByFechaCreacionDesc(organizadorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * RF58: listado global para el panel admin. En Mongo no hay JOIN con users-ms:
     * el correo se resuelve aparte y se deja en {@code organizadorEmail}.
     *
     * @return suscripciones más recientes primero
     */
    public List<SuscripcionResponse> listarTodas() {
        return suscripcionRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista el historial de una suscripción propia.
     *
     * @param organizadorId identificador del organizador
     * @param suscripcionId identificador de la suscripción
     * @return movimientos de la suscripción
     */
    public List<HistorialSuscripcionResponse> historialPropio(String organizadorId, Long suscripcionId) {
        obtenerPropia(organizadorId, suscripcionId);
        return historialSuscripcionRepository.findBySuscripcionIdOrderByFechaMovimientoDesc(suscripcionId).stream()
                .map(this::toHistorialResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista el historial de todas las suscripciones de un organizador.
     *
     * @param organizadorId identificador del organizador
     * @return movimientos del organizador
     */
    public List<HistorialSuscripcionResponse> historialPorOrganizador(String organizadorId) {
        return historialSuscripcionRepository.findByOrganizadorIdOrderByFechaMovimientoDesc(organizadorId)
                .stream().map(this::toHistorialResponse).collect(Collectors.toList());
    }

    /**
     * Cambia el estado de una suscripción y registra el movimiento.
     *
     * @param realizadoPor  UUID (o email, se resuelve) del admin que aplica el cambio
     * @param suscripcionId identificador de la suscripción
     * @param nuevoEstado   estado destino
     * @param motivo        motivo administrativo opcional, queda en el historial
     * @return suscripción actualizada
     */
    public SuscripcionResponse cambiarEstado(String realizadoPor, Long suscripcionId, EstadoSuscripcion nuevoEstado, String motivo) {
        Suscripcion suscripcion = obtenerEntidad(suscripcionId);
        EstadoSuscripcion anterior = suscripcion.getEstado();
        suscripcion.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoSuscripcion.CANCELADA) {
            suscripcion.setFechaCancelacion(java.time.LocalDateTime.now());
            suscripcion.setMotivoCancelacion(motivo);
        }
        if (nuevoEstado == EstadoSuscripcion.SUSPENDIDA) {
            suscripcion.setFechaSuspension(java.time.LocalDateTime.now());
            suscripcion.setMotivoSuspension(motivo);
        }
        suscripcion = suscripcionRepository.save(suscripcion);
        String realizadoPorId = organizerIdentityService.resolveUserId(realizadoPor);
        registrarHistorial(suscripcion, mapEstadoAMovimiento(nuevoEstado), anterior, nuevoEstado, null, realizadoPorId, motivo);
        return toResponse(suscripcion);
    }

    /**
     * Indica si el organizador puede crear un evento según su cupo mensual.
     *
     * @param organizadorId identificador del organizador
     * @return cupo, límite y si puede crear
     */
    public PuedeCrearEventoResponse puedeCrearEvento(String organizadorId) {
        return suscripcionRepository
                .findFirstByOrganizadorIdAndEstadoOrderByFechaCreacionDesc(organizadorId, EstadoSuscripcion.ACTIVA)
                .map(s -> {
                    resetPeriodoSiCorresponde(s);
                    return PuedeCrearEventoResponse.builder()
                            .puedeCrear(s.puedeCrearEvento())
                            .eventosCreadosMes(s.getEventosCreadosPeriodo())
                            .limiteEventosMes(s.getLimiteEventosAplicado())
                            .planNombre(s.getPlanNombre())
                            .build();
                })
                .orElseGet(() -> PuedeCrearEventoResponse.builder()
                        .puedeCrear(false)
                        .eventosCreadosMes(0)
                        .limiteEventosMes(0)
                        .planNombre(null)
                        .build());
    }

    /**
     * Consume un cupo de evento del período si la suscripción lo permite.
     *
     * @param organizadorId identificador del organizador
     */
    public void registrarEventoCreado(String organizadorId) {
        Suscripcion suscripcion = suscripcionRepository
                .findFirstByOrganizadorIdAndEstadoOrderByFechaCreacionDesc(organizadorId, EstadoSuscripcion.ACTIVA)
                .orElseThrow(() -> new SuscripcionNotFoundException(
                        "El organizador " + organizadorId + " no tiene una suscripcion activa"));
        resetPeriodoSiCorresponde(suscripcion);
        if (!suscripcion.puedeCrearEvento()) {
            throw new LimiteEventosExcedidoException(organizadorId, suscripcion.getLimiteEventosAplicado());
        }
        suscripcionRepository.incrementarEventosCreados(suscripcion.getId());
    }

    /**
     * Asigna el plan gratuito inicial si el organizador aún no tiene suscripción.
     *
     * @param organizadorId identificador del organizador
     * @return suscripción existente o la asignada
     */
    public SuscripcionResponse asignarPlanGratuito(String organizadorId) {
        var existente = suscripcionRepository.findFirstByOrganizadorIdOrderByFechaCreacionDesc(organizadorId);
        if (existente.isPresent()) {
            return toResponse(existente.get());
        }

        Plan planGratuito = planRepository.findFirstByEsPlanInicialTrueAndActivoTrue()
                .or(() -> planRepository.findFirstByEsGratuitoTrueAndActivoTrue())
                .orElseThrow(() -> new IllegalStateException("No hay un plan gratuito inicial configurado"));

        LocalDate hoy = LocalDate.now();
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setOrganizadorId(organizadorId);
        suscripcion.aplicarTerminos(planGratuito);
        suscripcion.setFechaInicio(hoy);
        suscripcion.setFechaFin(hoy.plusDays(planGratuito.getDuracionDias()));
        suscripcion.setPeriodoInicio(hoy.withDayOfMonth(1));
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setEventosCreadosPeriodo(0);
        suscripcion.setOrigen(OrigenSuscripcion.ASIGNACION_INICIAL);
        suscripcion.setRenovacionAutomatica(false);
        suscripcion = suscripcionRepository.save(suscripcion);

        registrarHistorial(suscripcion, TipoMovimiento.ASIGNACION_INICIAL, null, EstadoSuscripcion.ACTIVA, null, null, null);
        log.info("Plan gratuito inicial asignado a {}", organizadorId);
        return toResponse(suscripcion);
    }

    /**
     * Obtiene la entidad de suscripción o lanza si no existe.
     *
     * @param suscripcionId identificador de la suscripción
     * @return entidad persistida
     */
    public Suscripcion obtenerEntidad(Long suscripcionId) {
        return suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new SuscripcionNotFoundException(suscripcionId));
    }

    /**
     * Obtiene una suscripción y valida que pertenezca al organizador.
     *
     * @param organizadorId identificador del organizador
     * @param suscripcionId identificador de la suscripción
     * @return suscripción propia
     */
    public Suscripcion obtenerPropia(String organizadorId, Long suscripcionId) {
        Suscripcion suscripcion = obtenerEntidad(suscripcionId);
        if (!suscripcion.getOrganizadorId().equals(organizadorId)) {
            throw new AccessDeniedException("No tienes acceso a esta suscripcion");
        }
        return suscripcion;
    }

    /**
     * Reinicia el contador mensual si el período ya no corresponde al mes actual.
     *
     * @param s suscripción a revisar
     */
    private void resetPeriodoSiCorresponde(Suscripcion s) {
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        if (s.getPeriodoInicio() == null || s.getPeriodoInicio().isBefore(inicioMes)) {
            s.setPeriodoInicio(inicioMes);
            s.setEventosCreadosPeriodo(0);
            suscripcionRepository.save(s);
        }
    }

    /**
     * Persiste un movimiento en el historial de la suscripción.
     *
     * @param suscripcion    suscripción afectada
     * @param tipo           tipo de movimiento
     * @param anterior       estado previo
     * @param nuevo          estado nuevo
     * @param planAnteriorId plan anterior, o {@code null}
     * @param realizadoPor   UUID del admin que aplicó el cambio, o {@code null} si fue el propio organizador/sistema
     * @param notas          motivo u observación opcional
     */
    private void registrarHistorial(Suscripcion suscripcion, TipoMovimiento tipo,
                                    EstadoSuscripcion anterior, EstadoSuscripcion nuevo, Long planAnteriorId,
                                    String realizadoPor, String notas) {
        HistorialSuscripcion h = new HistorialSuscripcion();
        h.setSuscripcionId(suscripcion.getId());
        h.setOrganizadorId(suscripcion.getOrganizadorId());
        h.setTipoMovimiento(tipo);
        h.setPlanAnteriorId(planAnteriorId);
        h.setPlanAnteriorNombre(nombrePlan(planAnteriorId));
        h.setPlanNuevoId(suscripcion.getPlanId());
        h.setPlanNuevoNombre(suscripcion.getPlanNombre());
        h.setEstadoAnterior(anterior != null ? anterior.name() : null);
        h.setEstadoNuevo(nuevo != null ? nuevo.name() : null);
        h.setFechaFinNueva(suscripcion.getFechaFin());
        h.setRealizadoPor(realizadoPor);
        h.setNotas(notas);
        historialSuscripcionRepository.save(h);
    }

    /**
     * Traduce un estado de suscripción al tipo de movimiento de historial.
     *
     * @param estado estado destino
     * @return tipo de movimiento
     */
    private TipoMovimiento mapEstadoAMovimiento(EstadoSuscripcion estado) {
        return switch (estado) {
            case CANCELADA -> TipoMovimiento.CANCELACION;
            case SUSPENDIDA -> TipoMovimiento.SUSPENSION;
            case VENCIDA -> TipoMovimiento.VENCIMIENTO;
            case ACTIVA -> TipoMovimiento.REACTIVACION;
        };
    }

    /**
     * Convierte la suscripción a DTO de respuesta.
     *
     * @param s entidad persistida
     * @return DTO de respuesta
     */
    private SuscripcionResponse toResponse(Suscripcion s) {
        return SuscripcionResponse.builder()
                .id(s.getId())
                .organizadorId(s.getOrganizadorId())
                .organizadorEmail(organizerIdentityService.resolveEmail(s.getOrganizadorId()))
                .planId(s.getPlanId())
                .planNombre(s.getPlanNombre())
                .precioAplicado(s.getPrecioAplicado())
                .limiteEventosAplicado(s.getLimiteEventosAplicado())
                .porcentajeComisionAplicado(s.getPorcentajeComisionAplicado())
                .fechaInicio(s.getFechaInicio())
                .fechaFin(s.getFechaFin())
                .estado(s.getEstado())
                .eventosCreadosMes(s.getEventosCreadosPeriodo())
                .limiteEventosMes(s.getLimiteEventosAplicado())
                .renovacionAutomatica(s.getRenovacionAutomatica())
                .fechaCreacion(s.getFechaCreacion())
                .build();
    }

    /**
     * Convierte un movimiento de historial a DTO de respuesta.
     *
     * @param h movimiento persistido
     * @return DTO de historial
     */
    private HistorialSuscripcionResponse toHistorialResponse(HistorialSuscripcion h) {
        return HistorialSuscripcionResponse.builder()
                .id(h.getId())
                .suscripcionId(h.getSuscripcionId())
                .tipoMovimiento(h.getTipoMovimiento())
                .planAnteriorId(h.getPlanAnteriorId())
                .planAnteriorNombre(h.getPlanAnteriorNombre() != null
                        ? h.getPlanAnteriorNombre() : nombrePlan(h.getPlanAnteriorId()))
                .planNuevoId(h.getPlanNuevoId())
                .planNuevoNombre(h.getPlanNuevoNombre() != null
                        ? h.getPlanNuevoNombre() : nombrePlan(h.getPlanNuevoId()))
                .estadoAnterior(h.getEstadoAnterior())
                .estadoNuevo(h.getEstadoNuevo())
                .notas(h.getNotas())
                .realizadoPor(h.getRealizadoPor())
                .realizadoPorEmail(organizerIdentityService.resolveEmail(h.getRealizadoPor()))
                .fechaMovimiento(h.getFechaMovimiento())
                .build();
    }


    /**
     * Resuelve el nombre de un plan por su identificador.
     *
     * @param planId identificador del plan
     * @return nombre del plan, o {@code null} si no existe
     */
    private String nombrePlan(Long planId) {
        if (planId == null) {
            return null;
        }
        return planRepository.findById(planId).map(Plan::getNombre).orElse(null);
    }
}
