package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.Plan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends MongoRepository<Plan, Long> {

    List<Plan> findByActivoTrue();

    Optional<Plan> findFirstByEsPlanInicialTrueAndActivoTrue();

    Optional<Plan> findFirstByEsGratuitoTrueAndActivoTrue();

    List<Plan> findByEsPlanInicialTrue();
}
