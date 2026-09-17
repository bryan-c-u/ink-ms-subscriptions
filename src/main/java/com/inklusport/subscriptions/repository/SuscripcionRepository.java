package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.Suscripcion;
import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository extends MongoRepository<Suscripcion, Long>, SuscripcionRepositoryCustom {

    Optional<Suscripcion> findFirstByOrganizadorIdOrderByFechaCreacionDesc(String organizadorId);

    Optional<Suscripcion> findFirstByOrganizadorIdAndEstadoOrderByFechaCreacionDesc(
            String organizadorId, EstadoSuscripcion estado);

    List<Suscripcion> findByOrganizadorIdOrderByFechaCreacionDesc(String organizadorId);

    List<Suscripcion> findByEstadoAndFechaFinBefore(EstadoSuscripcion estado, LocalDate fecha);

    List<Suscripcion> findByEstadoAndFechaFinBetween(EstadoSuscripcion estado, LocalDate desde, LocalDate hasta);

    List<Suscripcion> findByEstado(EstadoSuscripcion estado);

    List<Suscripcion> findAllByOrderByFechaCreacionDesc();
}
