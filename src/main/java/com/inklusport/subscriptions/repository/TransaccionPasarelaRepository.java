package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.TransaccionPasarela;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TransaccionPasarelaRepository extends MongoRepository<TransaccionPasarela, Long> {

    Optional<TransaccionPasarela> findByReferenciaExterna(String referenciaExterna);

    Optional<TransaccionPasarela> findByPagoExternoId(String pagoExternoId);

    Optional<TransaccionPasarela> findByPreferenciaId(String preferenciaId);
}
