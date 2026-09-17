package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.ReporteEventoItem;
import com.inklusport.subscriptions.dto.ReporteFinancieroResponse;
import com.inklusport.subscriptions.entity.PagoEvento;
import com.inklusport.subscriptions.entity.PagoSuscripcion;
import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.repository.PagoEventoRepository;
import com.inklusport.subscriptions.repository.PagoSuscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de reportes financieros de eventos y suscripciones.
 *
 * El agrupado por evento se hace en memoria sobre los pagos APROBADOS del rango: son
 * pocos documentos por período y así el reporte no depende de un pipeline de agregación
 * ni de cómo Mongo represente los importes decimales.
 */
@Service
@RequiredArgsConstructor
public class ReporteFinancieroService {

    private final PagoEventoRepository pagoEventoRepository;
    private final PagoSuscripcionRepository pagoSuscripcionRepository;

    /**
     * Genera el reporte financiero de un organizador en un rango de fechas.
     *
     * @param organizadorId identificador del organizador
     * @param desde         inicio del período
     * @param hasta         fin del período
     * @return reporte de ingresos por eventos
     */
    public ReporteFinancieroResponse reporteOrganizador(String organizadorId, LocalDateTime desde, LocalDateTime hasta) {
        List<PagoEvento> pagos = pagoEventoRepository.findByOrganizadorIdAndEstadoAndFechaPagoBetween(
                organizadorId, EstadoPago.APROBADO, desde, hasta);
        return construir(desde, hasta, agruparPorEvento(pagos), BigDecimal.ZERO);
    }

    /**
     * Genera el reporte financiero global, incluyendo suscripciones.
     *
     * @param desde inicio del período
     * @param hasta fin del período
     * @return reporte consolidado de la plataforma
     */
    public ReporteFinancieroResponse reporteAdmin(LocalDateTime desde, LocalDateTime hasta) {
        BigDecimal ingresosPorSuscripciones = pagoSuscripcionRepository
                .findByEstadoAndFechaPagoBetween(EstadoPago.APROBADO, desde, hasta).stream()
                .map(PagoSuscripcion::getMonto)
                .reduce(BigDecimal.ZERO, ReporteFinancieroService::sumar);
        List<PagoEvento> pagos = pagoEventoRepository.findByEstadoAndFechaPagoBetween(
                EstadoPago.APROBADO, desde, hasta);
        return construir(desde, hasta, agruparPorEvento(pagos), ingresosPorSuscripciones);
    }

    /**
     * Agrupa los pagos aprobados por evento acumulando inscritos, monto y comisión.
     *
     * @param pagos pagos de evento del período
     * @return detalle por evento
     */
    private List<ReporteEventoItem> agruparPorEvento(List<PagoEvento> pagos) {
        Map<String, Acumulado> porEvento = new LinkedHashMap<>();
        for (PagoEvento pago : pagos) {
            porEvento.computeIfAbsent(pago.getEventoId(), k -> new Acumulado()).sumar(pago);
        }

        List<ReporteEventoItem> detalle = new ArrayList<>(porEvento.size());
        porEvento.forEach((eventoId, acumulado) -> detalle.add(ReporteEventoItem.builder()
                .eventoId(eventoId)
                .nombreEvento(acumulado.nombreEvento)
                .numeroInscritos(acumulado.inscritos)
                .montoTotal(acumulado.monto)
                .comisionEstimada(acumulado.comision)
                .build()));
        return detalle;
    }

    /**
     * Arma el DTO de reporte con totales y detalle por evento.
     *
     * @param desde                    inicio del período
     * @param hasta                    fin del período
     * @param detalle                  detalle por evento
     * @param ingresosPorSuscripciones ingresos de suscripciones
     * @return reporte financiero
     */
    private ReporteFinancieroResponse construir(LocalDateTime desde, LocalDateTime hasta,
                                                List<ReporteEventoItem> detalle, BigDecimal ingresosPorSuscripciones) {
        BigDecimal ingresosPorEventos = detalle.stream()
                .map(ReporteEventoItem::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long numeroInscritos = detalle.stream().mapToLong(ReporteEventoItem::getNumeroInscritos).sum();
        return ReporteFinancieroResponse.builder()
                .desde(desde)
                .hasta(hasta)
                .ingresosPorEventos(ingresosPorEventos)
                .ingresosPorSuscripciones(ingresosPorSuscripciones)
                .numeroInscritos(numeroInscritos)
                .detallePorEvento(detalle)
                .build();
    }

    private static BigDecimal sumar(BigDecimal total, BigDecimal valor) {
        return valor != null ? total.add(valor) : total;
    }

    /** Totales de un evento mientras se recorre la lista de pagos. */
    private static final class Acumulado {
        private long inscritos;
        private String nombreEvento;
        private BigDecimal monto = BigDecimal.ZERO;
        private BigDecimal comision = BigDecimal.ZERO;

        private void sumar(PagoEvento pago) {
            inscritos++;
            if (nombreEvento == null && pago.getNombreEvento() != null && !pago.getNombreEvento().isBlank()) {
                nombreEvento = pago.getNombreEvento();
            }
            monto = ReporteFinancieroService.sumar(monto, pago.getMonto());
            comision = ReporteFinancieroService.sumar(comision, pago.getComisionPlataforma());
        }
    }
}
