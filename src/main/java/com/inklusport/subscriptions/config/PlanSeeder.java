package com.inklusport.suscripciones.config;

import com.inklusport.suscripciones.entity.BeneficioPlan;
import com.inklusport.suscripciones.entity.Plan;
import com.inklusport.suscripciones.repository.BeneficioPlanRepository;
import com.inklusport.suscripciones.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * RF66: siembra el plan gratuito inicial (y un par de planes pagos de ejemplo) para que
 * exista algo que asignar automaticamente a los organizadores nuevos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanSeeder implements ApplicationRunner {

    private final PlanRepository planRepository;
    private final BeneficioPlanRepository beneficioPlanRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (planRepository.count() > 0) {
            return;
        }

        crearPlan("Gratuito", "Plan inicial para organizadores nuevos", BigDecimal.ZERO, 2,
                BigDecimal.ZERO, 365, List.of("Hasta 2 eventos por mes", "Funcionalidades basicas"));

        crearPlan("Basico", "Para organizadores en crecimiento", new BigDecimal("49900.00"), 10,
                new BigDecimal("5.00"), 30,
                List.of("Hasta 10 eventos por mes", "Soporte prioritario", "Reportes basicos"));

        crearPlan("Profesional", "Para organizadores con alto volumen de eventos", new BigDecimal("149900.00"), 50,
                new BigDecimal("3.00"), 30,
                List.of("Hasta 50 eventos por mes", "Comision reducida", "Reportes financieros avanzados",
                        "Soporte prioritario 24/7"));

        log.info("Planes iniciales creados: Gratuito, Basico, Profesional");
    }

    private void crearPlan(String nombre, String descripcion, BigDecimal precio, int limiteEventosMes,
                            BigDecimal porcentajeComision, int duracionDias, List<String> beneficios) {
        Plan plan = new Plan();
        plan.setNombre(nombre);
        plan.setDescripcion(descripcion);
        plan.setPrecio(precio);
        plan.setLimiteEventosMes(limiteEventosMes);
        plan.setPorcentajeComision(porcentajeComision);
        plan.setDuracionDias(duracionDias);
        plan.setActivo(true);
        plan = planRepository.save(plan);

        for (String beneficio : beneficios) {
            BeneficioPlan bp = new BeneficioPlan();
            bp.setPlan(plan);
            bp.setBeneficio(beneficio);
            beneficioPlanRepository.save(bp);
        }
    }
}
