package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.FuncionalidadPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuncionalidadPlanRepository extends JpaRepository<FuncionalidadPlan, Long> {

    List<FuncionalidadPlan> findByPlanId(Long planId);

    void deleteByPlanId(Long planId);
}
