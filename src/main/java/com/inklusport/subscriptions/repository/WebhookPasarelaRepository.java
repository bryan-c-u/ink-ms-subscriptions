package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.WebhookPasarela;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookPasarelaRepository extends JpaRepository<WebhookPasarela, Long> {

    Optional<WebhookPasarela> findFirstByPasarelaAndIdExterno(com.inklusport.subscriptions.enums.Pasarela pasarela, String idExterno);
}
