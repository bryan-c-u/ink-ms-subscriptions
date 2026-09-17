package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.PagoSuscripcion;
import com.inklusport.subscriptions.enums.EstadoPago;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoSuscripcionRepository extends MongoRepository<PagoSuscripcion, Long> {

    List<PagoSuscripcion> findBySuscripcionIdOrderByFechaPagoDesc(Long suscripcionId);

    List<PagoSuscripcion> findByOrganizadorIdOrderByFechaPagoDesc(String organizadorId);

    Optional<PagoSuscripcion> findByReferenciaTransaccion(String referenciaTransaccion);

    /** RF62: base de los ingresos por suscripciones; la suma se hace en el servicio. */
    List<PagoSuscripcion> findByEstadoAndFechaPagoBetween(EstadoPago estado, LocalDateTime desde, LocalDateTime hasta);
}
