package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.CrearSuscripcionRequest;
import com.inklusport.subscriptions.dto.PuedeCrearEventoResponse;
import com.inklusport.subscriptions.entity.Plan;
import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import com.inklusport.subscriptions.exception.PlanInactivoException;
import com.inklusport.subscriptions.repository.HistorialSuscripcionRepository;
import com.inklusport.subscriptions.repository.PlanRepository;
import com.inklusport.subscriptions.repository.SuscripcionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuscripcionServiceTest {

    @Mock
    private SuscripcionRepository suscripcionRepository;
    @Mock
    private HistorialSuscripcionRepository historialSuscripcionRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private PlanService planService;
    @Mock
    private PagoSuscripcionService pagoSuscripcionService;

    @InjectMocks
    private SuscripcionService suscripcionService;

    @Test
    void puedeCrearEvento_sinSuscripcionActiva() {
        when(suscripcionRepository.findFirstByOrganizadorIdAndEstadoOrderByFechaCreacionDesc(
                "org-1", EstadoSuscripcion.ACTIVA)).thenReturn(Optional.empty());

        PuedeCrearEventoResponse response = suscripcionService.puedeCrearEvento("org-1");

        assertFalse(response.isPuedeCrear());
        assertEquals(0, response.getLimiteEventosMes());
    }

    @Test
    void crearSolicitud_rechazaPlanInactivo() {
        Plan plan = new Plan();
        plan.setId(3L);
        plan.setActivo(false);
        CrearSuscripcionRequest request = new CrearSuscripcionRequest();
        request.setPlanId(3L);
        when(planService.obtenerEntidad(3L)).thenReturn(plan);

        assertThrows(PlanInactivoException.class, () -> suscripcionService.crearSolicitud("org-1", request));
        verify(suscripcionRepository, never()).save(any());
    }
}
