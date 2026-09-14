package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.HistorialPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialPlanRepository extends JpaRepository<HistorialPlan, Long> {

    List<HistorialPlan> findByPlanIdOrderByFechaModificacionDesc(Long planId);
}
