package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.HistorialPlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HistorialPlanRepository extends MongoRepository<HistorialPlan, Long> {

    List<HistorialPlan> findByPlanIdOrderByFechaModificacionDesc(Long planId);
}
