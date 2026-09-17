package com.inklusport.subscriptions.config;

import com.inklusport.subscriptions.entity.DocumentoSecuencial;
import com.inklusport.subscriptions.entity.Secuencia;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Asigna el {@code _id} numérico a cualquier documento nuevo que implemente
 * {@link DocumentoSecuencial}, usando un contador atómico por colección
 * ({@code contador_secuencia}). Así los servicios siguen llamando a
 * {@code repository.save(new Entidad())} sin gestionar el identificador.
 *
 * {@link ObjectProvider} evita el ciclo MappingMongoConverter → callback → MongoTemplate.
 */
@Component
public class SecuenciaIdCallback implements BeforeConvertCallback<DocumentoSecuencial> {

    private final ObjectProvider<MongoOperations> mongoOperations;

    public SecuenciaIdCallback(ObjectProvider<MongoOperations> mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public DocumentoSecuencial onBeforeConvert(DocumentoSecuencial entidad, String coleccion) {
        if (entidad.getId() == null) {
            entidad.setId(siguienteValor(coleccion));
        }
        return entidad;
    }

    private Long siguienteValor(String coleccion) {
        Secuencia secuencia = mongoOperations.getObject().findAndModify(
                Query.query(Criteria.where("_id").is(coleccion)),
                new Update().inc("valor", 1L),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Secuencia.class);
        return secuencia != null && secuencia.getValor() != null ? secuencia.getValor() : 1L;
    }
}
