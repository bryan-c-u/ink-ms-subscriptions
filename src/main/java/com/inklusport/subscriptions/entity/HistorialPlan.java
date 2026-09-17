package com.inklusport.subscriptions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/** RF65: auditoría de cambios de catálogo (precio, límites, comisión, beneficios). */
@Document(collection = "historial_plan")
@CompoundIndex(name = "idx_historial_plan", def = "{'plan_id': 1, 'fecha_modificacion': -1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPlan implements DocumentoSecuencial {

    @Id
    private Long id;

    @Field("plan_id")
    private Long planId;

    @Field("campo_modificado")
    private String campoModificado;

    @Field("valor_anterior")
    private String valorAnterior;

    @Field("valor_nuevo")
    private String valorNuevo;

    @Field("modificado_por")
    private String modificadoPor;

    @CreatedDate
    @Field("fecha_modificacion")
    private LocalDateTime fechaModificacion;
}
