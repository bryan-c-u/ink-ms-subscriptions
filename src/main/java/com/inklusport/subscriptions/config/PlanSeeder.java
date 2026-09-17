package com.inklusport.subscriptions.config;

import com.inklusport.subscriptions.entity.BeneficioPlan;
import com.inklusport.subscriptions.entity.FuncionalidadPlan;
import com.inklusport.subscriptions.entity.Plan;
import com.inklusport.subscriptions.repository.BeneficioPlanRepository;
import com.inklusport.subscriptions.repository.FuncionalidadPlanRepository;
import com.inklusport.subscriptions.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * RF54 / RF64: catálogo inicial de planes.
 *
 * En MySQL los planes semilla los insertaba {@code init-mysql/10-subscriptions-schema.sql}.
 * Los scripts de {@code docker-entrypoint-initdb.d} de Mongo solo corren cuando el volumen
 * está vacío, así que el catálogo se siembra desde la aplicación: si la colección
 * {@code plan} tiene algo, no se toca nada.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class PlanSeeder implements ApplicationRunner {

    private final PlanRepository planRepository;
    private final BeneficioPlanRepository beneficioPlanRepository;
    private final FuncionalidadPlanRepository funcionalidadPlanRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (planRepository.count() > 0) {
            log.debug("Catalogo de planes ya poblado; el seeder no inserta nada");
            return;
        }

        Plan gratuito = crearPlan("Básico Gratuito",
                "Plan inicial para organizadores nuevos. Eventos y funcionalidades limitados.",
                BigDecimal.ZERO, 2, new BigDecimal("15.00"), true, true);
        guardarBeneficios(gratuito, List.of(
                "Hasta 2 eventos por mes",
                "Comisión del 15% en eventos pagos",
                "Soporte por correo"));
        guardarFuncionalidades(gratuito, true, false, false);

        Plan pro = crearPlan("Pro",
                "Para organizadores con mayor volumen de eventos mensuales.",
                new BigDecimal("79900.00"), 20, new BigDecimal("10.00"), false, false);
        guardarBeneficios(pro, List.of(
                "Hasta 20 eventos por mes",
                "Comisión del 10% en eventos pagos",
                "Reportes financieros básicos",
                "Soporte prioritario"));
        guardarFuncionalidades(pro, true, true, true);

        Plan enterprise = crearPlan("Enterprise",
                "Plan ilimitado para organizaciones con alto volumen.",
                new BigDecimal("249900.00"), null, new BigDecimal("5.00"), false, false);
        guardarBeneficios(enterprise, List.of(
                "Eventos ilimitados",
                "Comisión del 5% en eventos pagos",
                "Reportes financieros avanzados",
                "Soporte dedicado"));
        guardarFuncionalidades(enterprise, true, true, true);

        log.info("Catalogo inicial de planes sembrado: Basico Gratuito, Pro y Enterprise");
    }

    private Plan crearPlan(String nombre, String descripcion, BigDecimal precio, Integer limiteEventosMes,
                           BigDecimal porcentajeComision, boolean gratuito, boolean planInicial) {
        Plan plan = new Plan();
        plan.setNombre(nombre);
        plan.setDescripcion(descripcion);
        plan.setPrecio(precio);
        plan.setMoneda("COP");
        plan.setLimiteEventosMes(limiteEventosMes);
        plan.setPorcentajeComision(porcentajeComision);
        plan.setDuracionDias(30);
        plan.setActivo(true);
        plan.setEsGratuito(gratuito);
        plan.setEsPlanInicial(planInicial);
        return planRepository.save(plan);
    }

    private void guardarBeneficios(Plan plan, List<String> beneficios) {
        int orden = 0;
        for (String texto : beneficios) {
            BeneficioPlan beneficio = new BeneficioPlan();
            beneficio.setPlanId(plan.getId());
            beneficio.setBeneficio(texto);
            beneficio.setOrden(++orden);
            beneficioPlanRepository.save(beneficio);
        }
    }

    private void guardarFuncionalidades(Plan plan, boolean eventosPagos, boolean reportes, boolean soporte) {
        guardarFuncionalidad(plan, "EVENTOS_PAGOS", "Crear eventos de pago", eventosPagos);
        guardarFuncionalidad(plan, "REPORTES_FINANCIEROS", "Reportes financieros detallados", reportes);
        guardarFuncionalidad(plan, "SOPORTE_PRIORITARIO", "Soporte prioritario", soporte);
    }

    private void guardarFuncionalidad(Plan plan, String codigo, String nombre, boolean habilitada) {
        FuncionalidadPlan funcionalidad = new FuncionalidadPlan();
        funcionalidad.setPlanId(plan.getId());
        funcionalidad.setCodigo(codigo);
        funcionalidad.setNombre(nombre);
        funcionalidad.setHabilitada(habilitada);
        funcionalidadPlanRepository.save(funcionalidad);
    }
}
