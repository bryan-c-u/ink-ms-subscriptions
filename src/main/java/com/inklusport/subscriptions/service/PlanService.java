package com.inklusport.suscripciones.service;

import com.inklusport.suscripciones.dto.PlanRequest;
import com.inklusport.suscripciones.dto.PlanResponse;
import com.inklusport.suscripciones.entity.BeneficioPlan;
import com.inklusport.suscripciones.entity.Plan;
import com.inklusport.suscripciones.enums.EstadoSuscripcion;
import com.inklusport.suscripciones.exception.PlanNotFoundException;
import com.inklusport.suscripciones.repository.BeneficioPlanRepository;
import com.inklusport.suscripciones.repository.PlanRepository;
import com.inklusport.suscripciones.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** RF56, RF67: consulta publica de planes y administracion de planes. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final PlanRepository planRepository;
    private final BeneficioPlanRepository beneficioPlanRepository;
    private final SuscripcionRepository suscripcionRepository;

    @Transactional(readOnly = true)
    public List<PlanResponse> listarActivos() {
        return planRepository.findByActivoTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listarTodos() {
        return planRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanResponse obtenerPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public Plan obtenerEntidad(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new PlanNotFoundException(id));
    }

    @Transactional
    public PlanResponse crear(PlanRequest request) {
        Plan plan = new Plan();
        plan.setNombre(request.getNombre());
        plan.setDescripcion(request.getDescripcion());
        plan.setPrecio(request.getPrecio());
        plan.setLimiteEventosMes(request.getLimiteEventosMes());
        plan.setPorcentajeComision(request.getPorcentajeComision());
        plan.setDuracionDias(request.getDuracionDias());
        plan.setActivo(true);
        plan = planRepository.save(plan);

        guardarBeneficios(plan, request.getBeneficios());
        log.info("Plan creado: {}", plan.getNombre());
        return toResponse(plan);
    }

    /**
     * RF67: si el plan ya tiene suscripciones activas, no se permite modificar sus
     * terminos comerciales (precio, limite, comision, duracion) para no afectar a los
     * organizadores que ya lo tienen contratado hasta el fin de su ciclo. Solo se
     * pueden actualizar descripcion y beneficios en ese caso.
     */
    @Transactional
    public PlanResponse actualizar(Long id, PlanRequest request) {
        Plan plan = obtenerEntidad(id);
        boolean tieneSuscripcionesActivas = !suscripcionRepository
                .findByEstado(EstadoSuscripcion.ACTIVA).stream()
                .filter(s -> s.getPlan().getId().equals(id))
                .toList()
                .isEmpty();

        if (tieneSuscripcionesActivas) {
            if (terminosComercialesCambiaron(plan, request)) {
                throw new IllegalStateException(
                        "El plan tiene organizadores con suscripcion activa: no se pueden modificar precio, " +
                                "limite de eventos, comision ni duracion hasta que finalicen su ciclo actual. " +
                                "Solo se puede actualizar la descripcion y los beneficios.");
            }
        } else {
            plan.setPrecio(request.getPrecio());
            plan.setLimiteEventosMes(request.getLimiteEventosMes());
            plan.setPorcentajeComision(request.getPorcentajeComision());
            plan.setDuracionDias(request.getDuracionDias());
        }

        plan.setNombre(request.getNombre());
        plan.setDescripcion(request.getDescripcion());
        plan = planRepository.save(plan);

        beneficioPlanRepository.deleteByPlanId(plan.getId());
        guardarBeneficios(plan, request.getBeneficios());

        return toResponse(plan);
    }

    @Transactional
    public PlanResponse desactivar(Long id) {
        Plan plan = obtenerEntidad(id);
        plan.setActivo(false);
        planRepository.save(plan);
        log.info("Plan desactivado: {}", plan.getNombre());
        return toResponse(plan);
    }

    private boolean terminosComercialesCambiaron(Plan plan, PlanRequest request) {
        return plan.getPrecio().compareTo(request.getPrecio()) != 0
                || !plan.getLimiteEventosMes().equals(request.getLimiteEventosMes())
                || plan.getPorcentajeComision().compareTo(request.getPorcentajeComision()) != 0
                || !plan.getDuracionDias().equals(request.getDuracionDias());
    }

    private void guardarBeneficios(Plan plan, List<String> beneficios) {
        if (beneficios == null) {
            return;
        }
        for (String beneficio : beneficios) {
            if (beneficio == null || beneficio.isBlank()) {
                continue;
            }
            BeneficioPlan bp = new BeneficioPlan();
            bp.setPlan(plan);
            bp.setBeneficio(beneficio);
            beneficioPlanRepository.save(bp);
        }
    }

    private PlanResponse toResponse(Plan plan) {
        List<String> beneficios = beneficioPlanRepository.findByPlanId(plan.getId()).stream()
                .map(BeneficioPlan::getBeneficio)
                .collect(Collectors.toList());

        return PlanResponse.builder()
                .id(plan.getId())
                .nombre(plan.getNombre())
                .descripcion(plan.getDescripcion())
                .precio(plan.getPrecio())
                .limiteEventosMes(plan.getLimiteEventosMes())
                .porcentajeComision(plan.getPorcentajeComision())
                .duracionDias(plan.getDuracionDias())
                .activo(plan.getActivo())
                .fechaCreacion(plan.getFechaCreacion())
                .beneficios(beneficios)
                .build();
    }
}
