package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.TransaccionPasarela;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransaccionPasarelaRepository extends JpaRepository<TransaccionPasarela, Long> {

    Optional<TransaccionPasarela> findByReferenciaExterna(String referenciaExterna);

    Optional<TransaccionPasarela> findByPagoExternoId(String pagoExternoId);

    Optional<TransaccionPasarela> findByPreferenciaId(String preferenciaId);
}
