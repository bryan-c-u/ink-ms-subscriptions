package com.inklusport.subscriptions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String moneda = "COP";

    @Column(name = "limite_eventos_mes")
    private Integer limiteEventosMes;

    @Column(name = "porcentaje_comision", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComision = BigDecimal.ZERO;

    @Column(name = "duracion_dias", nullable = false)
    private Integer duracionDias = 30;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "es_gratuito", nullable = false)
    private Boolean esGratuito = false;

    @Column(name = "es_plan_inicial", nullable = false)
    private Boolean esPlanInicial = false;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "creado_por", length = 36)
    private String creadoPor;

    @Column(name = "actualizado_por", length = 36)
    private String actualizadoPor;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BeneficioPlan> beneficios = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FuncionalidadPlan> funcionalidades = new ArrayList<>();
}
