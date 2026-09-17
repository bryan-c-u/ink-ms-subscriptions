package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.Suscripcion;
import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;

@RequiredArgsConstructor
public class SuscripcionRepositoryImpl implements SuscripcionRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public void incrementarEventosCreados(Long id) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(id)),
                new Update().inc("eventos_creados_periodo", 1),
                Suscripcion.class);
    }

    @Override
    public void reiniciarContadorEventosMensual(LocalDate periodoInicio) {
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("estado").is(EstadoSuscripcion.ACTIVA.name())),
                new Update().set("eventos_creados_periodo", 0).set("periodo_inicio", periodoInicio),
                Suscripcion.class);
    }
}
