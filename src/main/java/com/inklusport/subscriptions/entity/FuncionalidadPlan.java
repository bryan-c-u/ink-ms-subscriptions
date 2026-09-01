package com.inklusport.subscriptions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "funcionalidad_plan", uniqueConstraints = {
        @UniqueConstraint(name = "uk_funcionalidad_plan", columnNames = {"plan_id", "codigo"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuncionalidadPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Boolean habilitada = true;

    @Column
    private Integer limite;
}
