package com.inklusport.suscripciones.repository;

import com.inklusport.suscripciones.entity.PagoSuscripcion;
import com.inklusport.suscripciones.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoSuscripcionRepository extends JpaRepository<PagoSuscripcion, Long> {

    List<PagoSuscripcion> findBySuscripcionIdOrderByFechaPagoDesc(Long suscripcionId);

    Optional<PagoSuscripcion> findByReferenciaTransaccion(String referenciaTransaccion);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM PagoSuscripcion p " +
            "WHERE p.estado = :estado AND p.fechaPago BETWEEN :desde AND :hasta")
    BigDecimal sumMontoByEstadoAndFechaPagoBetween(
            @Param("estado") EstadoPago estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
