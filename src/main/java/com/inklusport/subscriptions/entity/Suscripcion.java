package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import com.inklusport.subscriptions.enums.OrigenSuscripcion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "suscripcion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizador_id", nullable = false, length = 100)
    private String organizadorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "precio_aplicado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioAplicado;

    @Column(nullable = false, length = 3)
    private String moneda = "COP";

    @Column(name = "limite_eventos_aplicado")
    private Integer limiteEventosAplicado;

    @Column(name = "porcentaje_comision_aplicado", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComisionAplicado;

    @Column(name = "duracion_dias_aplicada", nullable = false)
    private Integer duracionDiasAplicada;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSuscripcion estado = EstadoSuscripcion.ACTIVA;

    @Column(name = "eventos_creados_periodo", nullable = false)
    private Integer eventosCreadosPeriodo = 0;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "renovacion_automatica", nullable = false)
    private Boolean renovacionAutomatica = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigenSuscripcion origen = OrigenSuscripcion.COMPRA;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Column(name = "fecha_suspension")
    private LocalDateTime fechaSuspension;

    @Column(name = "motivo_suspension", length = 500)
    private String motivoSuspension;

    @Column(name = "fecha_ultima_renovacion")
    private LocalDate fechaUltimaRenovacion;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public void aplicarTerminos(Plan plan) {
        this.plan = plan;
        this.precioAplicado = plan.getPrecio();
        this.moneda = plan.getMoneda() != null ? plan.getMoneda() : "COP";
        this.limiteEventosAplicado = plan.getLimiteEventosMes();
        this.porcentajeComisionAplicado = plan.getPorcentajeComision();
        this.duracionDiasAplicada = plan.getDuracionDias();
    }

    public boolean puedeCrearEvento() {
        if (limiteEventosAplicado == null) {
            return true;
        }
        return eventosCreadosPeriodo == null || eventosCreadosPeriodo < limiteEventosAplicado;
    }
}
