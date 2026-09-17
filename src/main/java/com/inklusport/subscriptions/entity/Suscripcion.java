package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import com.inklusport.subscriptions.enums.OrigenSuscripcion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RF56 / RF57 / RF58 / RF59 / RF64 — suscripción del organizador.
 * Los campos {@code *Aplicado} congelan las condiciones del ciclo vigente (RF65), por eso
 * el plan se guarda como referencia ({@code plan_id}) más el nombre del momento de la
 * contratación: cambiar el catálogo no altera un ciclo ya cobrado.
 */
@Document(collection = "suscripcion")
@CompoundIndex(name = "idx_suscripcion_estado_fin", def = "{'estado': 1, 'fecha_fin': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Suscripcion implements DocumentoSecuencial {

    @Id
    private Long id;

    @Indexed
    @Field("organizador_id")
    private String organizadorId;

    @Field("plan_id")
    private Long planId;

    /** Nombre del plan en el momento de aplicar los términos (snapshot de RF65). */
    @Field("plan_nombre")
    private String planNombre;

    @Field("precio_aplicado")
    private BigDecimal precioAplicado;

    private String moneda = "COP";

    @Field("limite_eventos_aplicado")
    private Integer limiteEventosAplicado;

    @Field("porcentaje_comision_aplicado")
    private BigDecimal porcentajeComisionAplicado;

    @Field("duracion_dias_aplicada")
    private Integer duracionDiasAplicada;

    @Field("fecha_inicio")
    private LocalDate fechaInicio;

    @Field("fecha_fin")
    private LocalDate fechaFin;

    private EstadoSuscripcion estado = EstadoSuscripcion.ACTIVA;

    @Field("eventos_creados_periodo")
    private Integer eventosCreadosPeriodo = 0;

    /** Inicio del mes/ciclo de conteo de eventos (RF58). */
    @Field("periodo_inicio")
    private LocalDate periodoInicio;

    @Field("renovacion_automatica")
    private Boolean renovacionAutomatica = false;

    private OrigenSuscripcion origen = OrigenSuscripcion.COMPRA;

    @Field("fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Field("motivo_cancelacion")
    private String motivoCancelacion;

    @Field("fecha_suspension")
    private LocalDateTime fechaSuspension;

    @Field("motivo_suspension")
    private String motivoSuspension;

    @Field("fecha_ultima_renovacion")
    private LocalDate fechaUltimaRenovacion;

    @CreatedDate
    @Field("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Field("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public void aplicarTerminos(Plan plan) {
        this.planId = plan.getId();
        this.planNombre = plan.getNombre();
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
