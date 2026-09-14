package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByActivoTrue();

    Optional<Plan> findFirstByEsPlanInicialTrueAndActivoTrue();

    Optional<Plan> findFirstByEsGratuitoTrueAndActivoTrue();
}
