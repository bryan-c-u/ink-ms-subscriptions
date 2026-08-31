package com.inklusport.suscripciones.repository;

import com.inklusport.suscripciones.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByActivoTrue();

    List<Plan> findByNombreIgnoreCase(String nombre);
}
