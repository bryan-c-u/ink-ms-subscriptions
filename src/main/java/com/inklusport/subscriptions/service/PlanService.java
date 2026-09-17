package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.PlanRequest;
import com.inklusport.subscriptions.dto.PlanResponse;
import com.inklusport.subscriptions.entity.BeneficioPlan;
import com.inklusport.subscriptions.entity.FuncionalidadPlan;
import com.inklusport.subscriptions.entity.HistorialPlan;
import com.inklusport.subscriptions.entity.Plan;
import com.inklusport.subscriptions.exception.PlanNotFoundException;
import com.inklusport.subscriptions.repository.BeneficioPlanRepository;
import com.inklusport.subscriptions.repository.FuncionalidadPlanRepository;
import com.inklusport.subscriptions.repository.HistorialPlanRepository;
import com.inklusport.subscriptions.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** RF54 / RF65: catálogo de planes. Los ciclos vigentes no se tocan: usan snapshot en suscripcion. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final PlanRepository planRepository;
    private final BeneficioPlanRepository beneficioPlanRepository;
    private final FuncionalidadPlanRepository funcionalidadPlanRepository;
    private final HistorialPlanRepository historialPlanRepository;

    /**
     * Lista los planes activos del catálogo.
     *
     * @return planes activos
     */
    public List<PlanResponse> listarActivos() {
        return planRepository.findByActivoTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Lista todos los planes, incluidos los inactivos.
     *
     * @return catálogo completo de planes
     */
    public List<PlanResponse> listarTodos() {
        return planRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Obtiene un plan por su identificador.
     *
     * @param id identificador del plan
     * @return plan encontrado
     */
    public PlanResponse obtenerPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    /**
     * Obtiene la entidad de plan o lanza si no existe.
     *
     * @param id identificador del plan
     * @return entidad persistida
     */
    public Plan obtenerEntidad(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new PlanNotFoundException(id));
    }

    /**
     * Crea un plan activo con sus beneficios.
     *
     * @param request datos del plan
     * @return plan creado
     */
    public PlanResponse crear(PlanRequest request) {
        Plan plan = new Plan();
        aplicarRequest(plan, request);
        plan.setActivo(true);
        if (Boolean.TRUE.equals(plan.getEsPlanInicial())) {
            desmarcarOtrosPlanesIniciales(null);
        }
        plan = planRepository.save(plan);
        guardarBeneficios(plan, request.getBeneficios());
        log.info("Plan creado: {}", plan.getNombre());
        return toResponse(plan);
    }

    /**
     * Actualiza un plan sin registrar actor.
     *
     * @param id      identificador del plan
     * @param request nuevos datos del plan
     * @return plan actualizado
     */
    public PlanResponse actualizar(Long id, PlanRequest request) {
        return actualizar(id, request, null);
    }

    /**
     * Actualiza un plan, registra cambios y reemplaza beneficios.
     *
     * @param id      identificador del plan
     * @param request nuevos datos del plan
     * @param actorId usuario que realiza el cambio, o {@code null}
     * @return plan actualizado
     */
    public PlanResponse actualizar(Long id, PlanRequest request, String actorId) {
        Plan plan = obtenerEntidad(id);
        registrarCambio(plan, "precio", String.valueOf(plan.getPrecio()), String.valueOf(request.getPrecio()), actorId);
        registrarCambio(plan, "limite_eventos_mes", String.valueOf(plan.getLimiteEventosMes()),
                String.valueOf(request.getLimiteEventosMes()), actorId);
        registrarCambio(plan, "porcentaje_comision", String.valueOf(plan.getPorcentajeComision()),
                String.valueOf(request.getPorcentajeComision()), actorId);
        registrarCambio(plan, "duracion_dias", String.valueOf(plan.getDuracionDias()),
                String.valueOf(request.getDuracionDias()), actorId);
        registrarCambio(plan, "descripcion", plan.getDescripcion(), request.getDescripcion(), actorId);

        aplicarRequest(plan, request);
        if (Boolean.TRUE.equals(plan.getEsPlanInicial())) {
            desmarcarOtrosPlanesIniciales(plan.getId());
        }
        plan = planRepository.save(plan);

        beneficioPlanRepository.deleteByPlanId(plan.getId());
        guardarBeneficios(plan, request.getBeneficios());
        return toResponse(plan);
    }

    /**
     * Desactiva un plan sin afectar ciclos vigentes.
     *
     * @param id identificador del plan
     * @return plan desactivado
     */
    public PlanResponse desactivar(Long id) {
        Plan plan = obtenerEntidad(id);
        plan.setActivo(false);
        planRepository.save(plan);
        log.info("Plan desactivado: {}", plan.getNombre());
        return toResponse(plan);
    }

    /**
     * El índice único de Mongo ({@code uk_plan_inicial}) solo admite un plan inicial.
     * Hay que apagar el anterior antes de guardar el nuevo.
     *
     * @param planActualId plan que se está marcando como inicial, o {@code null} si aún no tiene id
     */
    private void desmarcarOtrosPlanesIniciales(Long planActualId) {
        for (Plan otro : planRepository.findByEsPlanInicialTrue()) {
            if (planActualId != null && planActualId.equals(otro.getId())) {
                continue;
            }
            otro.setEsPlanInicial(false);
            planRepository.save(otro);
        }
    }

    /**
     * Copia los campos del request sobre la entidad de plan.
     *
     * @param plan    entidad a actualizar
     * @param request datos de entrada
     */
    private void aplicarRequest(Plan plan, PlanRequest request) {
        plan.setNombre(request.getNombre());
        plan.setDescripcion(request.getDescripcion());
        plan.setPrecio(request.getPrecio());
        plan.setLimiteEventosMes(request.getLimiteEventosMes());
        plan.setPorcentajeComision(request.getPorcentajeComision());
        plan.setDuracionDias(request.getDuracionDias());
        plan.setMoneda(request.getMoneda() != null ? request.getMoneda() : "COP");
        boolean gratuito = request.getPrecio() != null && request.getPrecio().compareTo(BigDecimal.ZERO) == 0;
        plan.setEsGratuito(request.getEsGratuito() != null ? request.getEsGratuito() : gratuito);
        if (request.getEsPlanInicial() != null) {
            plan.setEsPlanInicial(request.getEsPlanInicial());
        }
    }

    /**
     * Registra un cambio de campo en el historial si el valor cambió.
     *
     * @param plan     plan modificado
     * @param campo    nombre del campo
     * @param anterior valor previo
     * @param nuevo    valor nuevo
     * @param actorId  usuario que realiza el cambio
     */
    private void registrarCambio(Plan plan, String campo, String anterior, String nuevo, String actorId) {
        if (Objects.equals(anterior, nuevo)) {
            return;
        }
        HistorialPlan h = new HistorialPlan();
        h.setPlanId(plan.getId());
        h.setCampoModificado(campo);
        h.setValorAnterior(anterior);
        h.setValorNuevo(nuevo);
        h.setModificadoPor(actorId);
        historialPlanRepository.save(h);
    }

    /**
     * Reemplaza los beneficios del plan en el orden recibido.
     *
     * @param plan       plan destino
     * @param beneficios textos de beneficio
     */
    private void guardarBeneficios(Plan plan, List<String> beneficios) {
        if (beneficios == null) {
            return;
        }
        int orden = 0;
        for (String beneficio : beneficios) {
            if (beneficio == null || beneficio.isBlank()) {
                continue;
            }
            BeneficioPlan bp = new BeneficioPlan();
            bp.setPlanId(plan.getId());
            bp.setBeneficio(beneficio);
            bp.setOrden(++orden);
            beneficioPlanRepository.save(bp);
        }
    }

    /**
     * Convierte el plan a DTO incluyendo beneficios y funcionalidades.
     *
     * @param plan entidad persistida
     * @return DTO de respuesta
     */
    private PlanResponse toResponse(Plan plan) {
        List<String> beneficios = beneficioPlanRepository.findByPlanIdOrderByOrdenAsc(plan.getId()).stream()
                .map(BeneficioPlan::getBeneficio)
                .collect(Collectors.toList());
        List<String> funcionalidades = funcionalidadPlanRepository.findByPlanId(plan.getId()).stream()
                .filter(f -> Boolean.TRUE.equals(f.getHabilitada()))
                .map(FuncionalidadPlan::getCodigo)
                .collect(Collectors.toList());

        return PlanResponse.builder()
                .id(plan.getId())
                .nombre(plan.getNombre())
                .descripcion(plan.getDescripcion())
                .precio(plan.getPrecio())
                .moneda(plan.getMoneda())
                .limiteEventosMes(plan.getLimiteEventosMes())
                .porcentajeComision(plan.getPorcentajeComision())
                .duracionDias(plan.getDuracionDias())
                .activo(plan.getActivo())
                .esGratuito(plan.getEsGratuito())
                .esPlanInicial(plan.getEsPlanInicial())
                .fechaCreacion(plan.getFechaCreacion())
                .beneficios(beneficios)
                .funcionalidades(funcionalidades)
                .build();
    }
}
