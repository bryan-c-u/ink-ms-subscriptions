package com.inklusport.subscriptions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** RF54: beneficios visibles en el catálogo de planes. */
@Document(collection = "beneficio_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BeneficioPlan implements DocumentoSecuencial {

    @Id
    private Long id;

    @Indexed
    @Field("plan_id")
    private Long planId;

    private String beneficio;

    private Integer orden = 0;
}
