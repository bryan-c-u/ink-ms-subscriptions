package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.ReporteEventoItem;
import com.inklusport.subscriptions.dto.ReporteFinancieroResponse;
import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.repository.PagoEventoRepository;
import com.inklusport.subscriptions.repository.PagoSuscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteFinancieroService {

    private final PagoEventoRepository pagoEventoRepository;
    private final PagoSuscripcionRepository pagoSuscripcionRepository;

    @Transactional(readOnly = true)
    public ReporteFinancieroResponse reporteOrganizador(String organizadorId, LocalDateTime desde, LocalDateTime hasta) {
        return construir(desde, hasta,
                mapFilas(pagoEventoRepository.reportePorOrganizadorYEvento(organizadorId, EstadoPago.APROBADO, desde, hasta)),
                BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public ReporteFinancieroResponse reporteAdmin(LocalDateTime desde, LocalDateTime hasta) {
        BigDecimal ingresosPorSuscripciones = pagoSuscripcionRepository
                .sumMontoByEstadoAndFechaPagoBetween(EstadoPago.APROBADO, desde, hasta);
        return construir(desde, hasta,
                mapFilas(pagoEventoRepository.reporteGlobalPorEvento(EstadoPago.APROBADO, desde, hasta)),
                ingresosPorSuscripciones);
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
            BigDecimal montoTotal = toBigDecimal(fila[2]);
            BigDecimal comision = toBigDecimal(fila.length > 3 ? fila[3] : BigDecimal.ZERO);
            return ReporteEventoItem.builder()
                    .eventoId(eventoId)
                    .numeroInscritos(numeroInscritos)
                    .montoTotal(montoTotal)
                    .comisionEstimada(comision)
                    .build();
        }).collect(Collectors.toList());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(((Number) value).doubleValue());
    }
}
