package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.TipoMovimiento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private TipoMovimiento tipoMovimiento;

    @Column(name = "plan_anterior_id")
    private Long planAnteriorId;

    @Column(name = "plan_nuevo_id")
    private Long planNuevoId;

    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", length = 20)
    private String estadoNuevo;

    @Column(name = "fecha_fin_anterior")
    private LocalDate fechaFinAnterior;

    @Column(name = "fecha_fin_nueva")
    private LocalDate fechaFinNueva;

    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "realizado_por", length = 36)
    private String realizadoPor;

    @Column(length = 500)
    private String notas;

    @CreationTimestamp
    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private LocalDateTime fechaMovimiento;
}
