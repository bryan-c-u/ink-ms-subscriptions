package com.inklusport.suscripciones.entity;

import com.inklusport.suscripciones.enums.TipoMovimiento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_suscripcion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false)
    private TipoMovimiento tipoMovimiento;

    @Column(name = "plan_anterior_id")
    private Long planAnteriorId;

    @Column(name = "plan_nuevo_id")
    private Long planNuevoId;

    @CreationTimestamp
    @Column(name = "fecha_movimiento")
    private LocalDateTime fechaMovimiento;
}
