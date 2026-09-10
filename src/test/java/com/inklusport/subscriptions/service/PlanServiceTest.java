package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.PlanResponse;
import com.inklusport.subscriptions.entity.Plan;
import com.inklusport.subscriptions.exception.PlanNotFoundException;
import com.inklusport.subscriptions.repository.BeneficioPlanRepository;
import com.inklusport.subscriptions.repository.FuncionalidadPlanRepository;
import com.inklusport.subscriptions.repository.HistorialPlanRepository;
import com.inklusport.subscriptions.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;
    @Mock
    private BeneficioPlanRepository beneficioPlanRepository;
    @Mock
    private FuncionalidadPlanRepository funcionalidadPlanRepository;
    @Mock
    private HistorialPlanRepository historialPlanRepository;

    @InjectMocks
    private PlanService planService;

    @Test
    void obtenerEntidad_lanzaSiNoExiste() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        PlanNotFoundException error = assertThrows(PlanNotFoundException.class, () -> planService.obtenerEntidad(99L));
        assertTrue(error.getMessage().contains("99"));
    }

    @Test
    void listarActivos_mapeaNombreYPrecio() {
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setNombre("Pro");
        plan.setPrecio(new BigDecimal("49000"));
        plan.setActivo(true);
        when(planRepository.findByActivoTrue()).thenReturn(List.of(plan));
        when(beneficioPlanRepository.findByPlanIdOrderByOrdenAsc(1L)).thenReturn(List.of());
        when(funcionalidadPlanRepository.findByPlanId(1L)).thenReturn(List.of());

        List<PlanResponse> planes = planService.listarActivos();

        assertEquals(1, planes.size());
        assertEquals("Pro", planes.get(0).getNombre());
        assertEquals(new BigDecimal("49000"), planes.get(0).getPrecio());
    }
}
