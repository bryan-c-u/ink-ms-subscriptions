package com.inklusport.suscripciones.repository;

import com.inklusport.suscripciones.entity.BeneficioPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeneficioPlanRepository extends JpaRepository<BeneficioPlan, Long> {

    List<BeneficioPlan> findByPlanId(Long planId);

    void deleteByPlanId(Long planId);
}
