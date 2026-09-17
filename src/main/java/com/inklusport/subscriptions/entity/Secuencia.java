package com.inklusport.subscriptions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Contador por colección que reemplaza al AUTO_INCREMENT de MySQL. */
@Document(collection = "contador_secuencia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Secuencia {

    /** Nombre de la colección a la que pertenece el contador. */
    @Id
    private String id;

    private Long valor;
}
