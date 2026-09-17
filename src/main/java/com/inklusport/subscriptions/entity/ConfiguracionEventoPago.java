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

/** RF63 — evento gratuito o de pago (valor de inscripción). */
@Document(collection = "configuracion_evento_pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionEventoPago implements DocumentoSecuencial {

    @Id
    private Long id;

    @Indexed(unique = true)
    @Field("evento_id")
    private String eventoId;

    @Indexed
    @Field("organizador_id")
    private String organizadorId;

    @Field("es_pago")
    private Boolean esPago = false;

    @Field("valor_inscripcion")
    private BigDecimal valorInscripcion;

    private String moneda = "COP";

    /** Snapshot del plan del organizador al configurar el evento. */
    @Field("porcentaje_comision")
    private BigDecimal porcentajeComision;

    @CreatedDate
    @Field("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Field("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
