package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.HistorialSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialSuscripcionRepository extends JpaRepository<HistorialSuscripcion, Long> {

    List<HistorialSuscripcion> findBySuscripcionIdOrderByFechaMovimientoDesc(Long suscripcionId);

    List<HistorialSuscripcion> findBySuscripcion_OrganizadorIdOrderByFechaMovimientoDesc(String organizadorId);
}
