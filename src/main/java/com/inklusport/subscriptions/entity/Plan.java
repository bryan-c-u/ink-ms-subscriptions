package com.inklusport.subscriptions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RF54 / RF64 / RF65 — catálogo de planes.
 * {@code activo=false} oculta el plan a nuevos organizadores; las suscripciones ya
 * contratadas siguen usando el snapshot guardado en {@link Suscripcion}.
 */
@Document(collection = "plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan implements DocumentoSecuencial {

    @Id
    private Long id;

    @Indexed(unique = true)
    private String nombre;

    private String descripcion;

    private BigDecimal precio = BigDecimal.ZERO;

    private String moneda = "COP";

    @Field("limite_eventos_mes")
    private Integer limiteEventosMes;

    @Field("porcentaje_comision")
    private BigDecimal porcentajeComision = BigDecimal.ZERO;

    @Field("duracion_dias")
    private Integer duracionDias = 30;

    @Indexed
    private Boolean activo = true;

    @Field("es_gratuito")
    private Boolean esGratuito = false;

    /** RF64: se asigna al registrar un organizador. */
    @Field("es_plan_inicial")
    private Boolean esPlanInicial = false;

    @CreatedDate
    @Field("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Field("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Field("creado_por")
    private String creadoPor;

    @Field("actualizado_por")
    private String actualizadoPor;
}
