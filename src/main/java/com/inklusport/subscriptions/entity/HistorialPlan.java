package com.inklusport.subscriptions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "campo_modificado", nullable = false, length = 80)
    private String campoModificado;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    @Column(name = "modificado_por", length = 36)
    private String modificadoPor;

    @CreationTimestamp
    @Column(name = "fecha_modificacion", nullable = false, updatable = false)
    private LocalDateTime fechaModificacion;
}
