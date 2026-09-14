package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.BeneficioPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeneficioPlanRepository extends JpaRepository<BeneficioPlan, Long> {

    List<BeneficioPlan> findByPlanIdOrderByOrdenAsc(Long planId);

    List<BeneficioPlan> findByPlanId(Long planId);

    void deleteByPlanId(Long planId);
}
