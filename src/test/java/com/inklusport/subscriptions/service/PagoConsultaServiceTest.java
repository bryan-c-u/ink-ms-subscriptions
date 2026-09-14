package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.PagoEstadoResponse;
import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.exception.PagoGatewayException;
import com.inklusport.subscriptions.exception.PagoNotFoundException;
import com.inklusport.subscriptions.mercadopago.PaymentStatusResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoConsultaServiceTest {

    @Mock
    private PaymentGatewayClient paymentGatewayClient;
    @Mock
    private PagoSuscripcionService pagoSuscripcionService;
    @Mock
    private PagoEventoService pagoEventoService;

    @InjectMocks
    private PagoConsultaService pagoConsultaService;

    private static final String EMAIL = "org@inklusport.com";
    private static final String REF_SUS = "PS-10-2";

    private PagoEstadoResponse estado(EstadoPago e) {
        return PagoEstadoResponse.builder()
                .referencia(REF_SUS).tipo("SUSCRIPCION").pagoId(10L)
                .estado(e).monto(new BigDecimal("19900.00"))
                .suscripcionActiva(e == EstadoPago.APROBADO).reconciliadoAhora(false)
                .build();
    }

    @Test
    void referenciaConPrefijoDesconocido_lanzaNotFound() {
        assertThrows(PagoNotFoundException.class,
                () -> pagoConsultaService.consultarEstado(EMAIL, "XX-1", "999", false));
        verifyNoInteractions(paymentGatewayClient, pagoSuscripcionService, pagoEventoService);
    }

    @Test
    void pagoYaResuelto_noConsultaLaPasarela() {
        when(pagoSuscripcionService.estadoActual(EMAIL, REF_SUS, false)).thenReturn(estado(EstadoPago.APROBADO));

        PagoEstadoResponse res = pagoConsultaService.consultarEstado(EMAIL, REF_SUS, "555", false);

        assertEquals(EstadoPago.APROBADO, res.getEstado());
        assertFalse(res.isReconciliadoAhora());
        verifyNoInteractions(paymentGatewayClient);
    }

    @Test
    void pendienteSinPaymentId_devuelvePendienteSinReconciliar() {
        when(pagoSuscripcionService.estadoActual(EMAIL, REF_SUS, false)).thenReturn(estado(EstadoPago.PENDIENTE));

        PagoEstadoResponse res = pagoConsultaService.consultarEstado(EMAIL, REF_SUS, null, false);

        assertEquals(EstadoPago.PENDIENTE, res.getEstado());
        verifyNoInteractions(paymentGatewayClient);
        verify(pagoSuscripcionService, never()).confirmarPago(any());
    }

    @Test
    void pendienteConPaymentId_yPasarelaAprueba_reconciliaYReleeAprobado() {
        when(pagoSuscripcionService.estadoActual(EMAIL, REF_SUS, false))
                .thenReturn(estado(EstadoPago.PENDIENTE), estado(EstadoPago.APROBADO));
        when(paymentGatewayClient.consultarPago("555")).thenReturn(PaymentStatusResult.builder()
                .paymentIdExterno("555").referenciaExterna(REF_SUS).estado(EstadoPago.APROBADO)
                .montoPagado(new BigDecimal("19900.00")).metodoPago("visa").build());

        PagoEstadoResponse res = pagoConsultaService.consultarEstado(EMAIL, REF_SUS, "555", false);

        verify(pagoSuscripcionService).confirmarPago(any(PaymentStatusResult.class));
        assertEquals(EstadoPago.APROBADO, res.getEstado());
        assertTrue(res.isReconciliadoAhora());
    }

    @Test
    void pendienteConPaymentId_peroReferenciaNoCoincide_noConfirma() {
        when(pagoSuscripcionService.estadoActual(EMAIL, REF_SUS, false)).thenReturn(estado(EstadoPago.PENDIENTE));
        when(paymentGatewayClient.consultarPago("555")).thenReturn(PaymentStatusResult.builder()
                .paymentIdExterno("555").referenciaExterna("PS-99-2").estado(EstadoPago.APROBADO).build());

        PagoEstadoResponse res = pagoConsultaService.consultarEstado(EMAIL, REF_SUS, "555", false);

        verify(pagoSuscripcionService, never()).confirmarPago(any());
        assertFalse(res.isReconciliadoAhora());
        assertEquals(EstadoPago.PENDIENTE, res.getEstado());
    }

    @Test
    void pendienteConPaymentId_peroPasarelaFalla_devuelveEstadoLocalSinPropagar() {
        when(pagoSuscripcionService.estadoActual(EMAIL, REF_SUS, false)).thenReturn(estado(EstadoPago.PENDIENTE));
        when(paymentGatewayClient.consultarPago("555")).thenThrow(new PagoGatewayException("timeout"));

        PagoEstadoResponse res = pagoConsultaService.consultarEstado(EMAIL, REF_SUS, "555", false);

        assertEquals(EstadoPago.PENDIENTE, res.getEstado());
        assertFalse(res.isReconciliadoAhora());
        verify(pagoSuscripcionService, never()).confirmarPago(any());
    }

    @Test
    void referenciaDeEvento_enrutaAlServicioDeEventos() {
        when(pagoEventoService.estadoActual(eq(EMAIL), eq("PE-7"), anyBoolean()))
                .thenReturn(PagoEstadoResponse.builder().referencia("PE-7").tipo("EVENTO").pagoId(7L)
                        .estado(EstadoPago.PENDIENTE).monto(new BigDecimal("5000")).build());

        PagoEstadoResponse res = pagoConsultaService.consultarEstado(EMAIL, "PE-7", null, false);

        assertEquals("EVENTO", res.getTipo());
        verifyNoInteractions(pagoSuscripcionService);
    }
}
