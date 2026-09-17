package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.WebhookPasarela;
import com.inklusport.subscriptions.enums.Pasarela;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WebhookPasarelaRepository extends MongoRepository<WebhookPasarela, Long> {

    Optional<WebhookPasarela> findFirstByPasarelaAndIdExterno(Pasarela pasarela, String idExterno);
}
