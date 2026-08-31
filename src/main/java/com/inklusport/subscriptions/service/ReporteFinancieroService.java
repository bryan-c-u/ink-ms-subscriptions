package com.inklusport.suscripciones.service;

import com.inklusport.suscripciones.dto.ReporteEventoItem;
import com.inklusport.suscripciones.dto.ReporteFinancieroResponse;
import com.inklusport.suscripciones.enums.EstadoPago;
import com.inklusport.suscripciones.repository.ConfiguracionEventoPagoRepository;
import com.inklusport.suscripciones.repository.PagoEventoRepository;
import com.inklusport.suscripciones.repository.PagoSuscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** RF64: reportes financieros para organizador (sus eventos) y administrador (globales). */
@Service
@RequiredArgsConstructor
public class ReporteFinancieroService {

    private final PagoEventoRepository pagoEventoRepository;
    private final PagoSuscripcionRepository pagoSuscripcionRepository;
    private final ConfiguracionEventoPagoRepository configuracionEventoPagoRepository;

    @Transactional(readOnly = true)
    public ReporteFinancieroResponse reporteOrganizador(String organizadorId, LocalDateTime desde, LocalDateTime hasta) {
        List<ReporteEventoItem> detalle = mapFilas(
                pagoEventoRepository.reportePorOrganizadorYEvento(organizadorId, EstadoPago.APROBADO, desde, hasta));

        return construir(desde, hasta, detalle, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public ReporteFinancieroResponse reporteAdmin(LocalDateTime desde, LocalDateTime hasta) {
        List<ReporteEventoItem> detalle = mapFilas(
                pagoEventoRepository.reporteGlobalPorEvento(EstadoPago.APROBADO, desde, hasta));

        BigDecimal ingresosPorSuscripciones = pagoSuscripcionRepository
                .sumMontoByEstadoAndFechaPagoBetween(EstadoPago.APROBADO, desde, hasta);

        return construir(desde, hasta, detalle, ingresosPorSuscripciones);
    }

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

    private List<ReporteEventoItem> mapFilas(List<Object[]> filas) {
        return filas.stream().map(fila -> {
            String eventoId = (String) fila[0];
            long numeroInscritos = ((Number) fila[1]).longValue();
            BigDecimal montoTotal = fila[2] instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) fila[2]).doubleValue());

            BigDecimal porcentajeComision = configuracionEventoPagoRepository.findByEventoId(eventoId)
                    .map(c -> c.getPorcentajeComision() != null ? c.getPorcentajeComision() : BigDecimal.ZERO)
                    .orElse(BigDecimal.ZERO);
            BigDecimal comisionEstimada = montoTotal
                    .multiply(porcentajeComision)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            return ReporteEventoItem.builder()
                    .eventoId(eventoId)
                    .numeroInscritos(numeroInscritos)
                    .montoTotal(montoTotal)
                    .comisionEstimada(comisionEstimada)
                    .build();
        }).collect(Collectors.toList());
    }
}
