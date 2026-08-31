package com.inklusport.suscripciones.repository;

import com.inklusport.suscripciones.entity.Suscripcion;
import com.inklusport.suscripciones.enums.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    Optional<Suscripcion> findFirstByOrganizadorIdOrderByFechaCreacionDesc(String organizadorId);

    Optional<Suscripcion> findFirstByOrganizadorIdAndEstadoOrderByFechaCreacionDesc(
            String organizadorId, EstadoSuscripcion estado);

    List<Suscripcion> findByOrganizadorIdOrderByFechaCreacionDesc(String organizadorId);

    List<Suscripcion> findByEstadoAndFechaFinBefore(EstadoSuscripcion estado, LocalDate fecha);

    List<Suscripcion> findByEstadoAndFechaFinBetween(EstadoSuscripcion estado, LocalDate desde, LocalDate hasta);

    List<Suscripcion> findByEstado(EstadoSuscripcion estado);

    @Modifying
    @Transactional
    @Query("UPDATE Suscripcion s SET s.eventosCreadosMes = s.eventosCreadosMes + 1 WHERE s.id = :id")
    void incrementarEventosCreados(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Suscripcion s SET s.eventosCreadosMes = 0 WHERE s.estado = 'ACTIVA'")
    void reiniciarContadorEventosMensual();
}
