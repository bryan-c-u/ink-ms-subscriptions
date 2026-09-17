package com.inklusport.subscriptions.repository;

import java.time.LocalDate;

/**
 * Actualizaciones que en MySQL eran {@code UPDATE ... SET} y en Mongo se resuelven con
 * operadores atómicos ({@code $inc}, {@code $set}) para no leer y reescribir el documento.
 */
public interface SuscripcionRepositoryCustom {

    /** Consume un cupo del período de la suscripción indicada. */
    void incrementarEventosCreados(Long id);

    /** Pone a cero el contador mensual de todas las suscripciones activas. */
    void reiniciarContadorEventosMensual(LocalDate periodoInicio);
}
