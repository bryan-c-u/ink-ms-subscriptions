package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.ConfiguracionEventoPago;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionEventoPagoRepository extends MongoRepository<ConfiguracionEventoPago, Long> {

    Optional<ConfiguracionEventoPago> findByEventoId(String eventoId);

    boolean existsByEventoId(String eventoId);

    List<ConfiguracionEventoPago> findByOrganizadorId(String organizadorId);
}
