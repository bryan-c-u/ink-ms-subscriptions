package com.inklusport.suscripciones.repository;

import com.inklusport.suscripciones.entity.PagoEvento;
import com.inklusport.suscripciones.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoEventoRepository extends JpaRepository<PagoEvento, Long> {

    List<PagoEvento> findByUsuarioIdOrderByFechaPagoDesc(String usuarioId);

    Optional<PagoEvento> findByReferenciaTransaccion(String referenciaTransaccion);

    Optional<PagoEvento> findByUsuarioIdAndEventoIdAndEstado(String usuarioId, String eventoId, EstadoPago estado);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM PagoEvento p " +
            "WHERE p.estado = :estado AND p.fechaPago BETWEEN :desde AND :hasta")
    BigDecimal sumMontoByEstadoAndFechaPagoBetween(
            @Param("estado") EstadoPago estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /**
     * Fila por evento: [eventoId, numeroInscritos, montoTotal]. No hay FK real entre
     * pago_evento y configuracion_evento_pago (evento_id es un id externo), por eso el
     * join se hace por igualdad de campo en la consulta en vez de una relacion JPA.
     */
    @Query("SELECT p.eventoId, COUNT(p), COALESCE(SUM(p.monto), 0) FROM PagoEvento p " +
            "WHERE p.estado = :estado AND p.fechaPago BETWEEN :desde AND :hasta " +
            "GROUP BY p.eventoId")
    List<Object[]> reporteGlobalPorEvento(
            @Param("estado") EstadoPago estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT p.eventoId, COUNT(p), COALESCE(SUM(p.monto), 0) FROM PagoEvento p, ConfiguracionEventoPago c " +
            "WHERE p.eventoId = c.eventoId AND c.organizadorId = :organizadorId " +
            "AND p.estado = :estado AND p.fechaPago BETWEEN :desde AND :hasta " +
            "GROUP BY p.eventoId")
    List<Object[]> reportePorOrganizadorYEvento(
            @Param("organizadorId") String organizadorId,
            @Param("estado") EstadoPago estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
