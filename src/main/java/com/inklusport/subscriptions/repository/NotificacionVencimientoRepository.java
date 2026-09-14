package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.NotificacionVencimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NotificacionVencimientoRepository extends JpaRepository<NotificacionVencimiento, Long> {

    Optional<NotificacionVencimiento> findBySuscripcionIdAndDiasAntesAndFechaProgramada(
            Long suscripcionId, Integer diasAntes, LocalDate fechaProgramada);

    List<NotificacionVencimiento> findByEstadoAndFechaProgramadaLessThanEqual(String estado, LocalDate fecha);
}
