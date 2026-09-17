package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.FuncionalidadPlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FuncionalidadPlanRepository extends MongoRepository<FuncionalidadPlan, Long> {

    List<FuncionalidadPlan> findByPlanId(Long planId);

    void deleteByPlanId(Long planId);
}
