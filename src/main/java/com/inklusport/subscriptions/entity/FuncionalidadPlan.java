package com.inklusport.subscriptions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** RF58 / RF54: funcionalidades y cupos extra del plan, además de eventos/mes. */
@Document(collection = "funcionalidad_plan")
@CompoundIndex(name = "uk_funcionalidad_plan", def = "{'plan_id': 1, 'codigo': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuncionalidadPlan implements DocumentoSecuencial {

    @Id
    private Long id;

    @Field("plan_id")
    private Long planId;

    /** EVENTOS_PAGOS, REPORTES_FINANCIEROS, SOPORTE_PRIORITARIO, ... */
    private String codigo;

    private String nombre;

    private Boolean habilitada = true;

    /** {@code null} = sin cupo numérico (flag on/off). */
    private Integer limite;
}
