package com.inklusport.suscripciones.repository;

import com.inklusport.suscripciones.entity.ComprobantePago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprobantePagoRepository extends JpaRepository<ComprobantePago, Long> {

    Optional<ComprobantePago> findByPagoEventoId(Long pagoEventoId);

    Optional<ComprobantePago> findByPagoSuscripcionId(Long pagoSuscripcionId);

    Optional<ComprobantePago> findByNumeroComprobante(String numeroComprobante);
}
