package com.inklusport.suscripciones.repository;

import com.inklusport.suscripciones.entity.ConfiguracionEventoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionEventoPagoRepository extends JpaRepository<ConfiguracionEventoPago, Long> {

    Optional<ConfiguracionEventoPago> findByEventoId(String eventoId);

    boolean existsByEventoId(String eventoId);

    List<ConfiguracionEventoPago> findByOrganizadorId(String organizadorId);
}
