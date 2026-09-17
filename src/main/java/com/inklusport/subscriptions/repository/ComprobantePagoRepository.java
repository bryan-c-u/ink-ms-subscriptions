package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.ComprobantePago;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ComprobantePagoRepository extends MongoRepository<ComprobantePago, Long> {

    Optional<ComprobantePago> findByPagoEventoId(Long pagoEventoId);

    Optional<ComprobantePago> findByPagoSuscripcionId(Long pagoSuscripcionId);

    Optional<ComprobantePago> findByNumeroComprobante(String numeroComprobante);
}
