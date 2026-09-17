package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.HistorialSuscripcion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HistorialSuscripcionRepository extends MongoRepository<HistorialSuscripcion, Long> {

    List<HistorialSuscripcion> findBySuscripcionIdOrderByFechaMovimientoDesc(Long suscripcionId);

    List<HistorialSuscripcion> findByOrganizadorIdOrderByFechaMovimientoDesc(String organizadorId);

    boolean existsBySuscripcionId(Long suscripcionId);
}
