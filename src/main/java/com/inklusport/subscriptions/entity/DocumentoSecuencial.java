package com.inklusport.subscriptions.entity;

/**
 * Documento cuyo {@code _id} es un entero incremental en lugar de un ObjectId.
 *
 * MongoDB no tiene AUTO_INCREMENT: el identificador lo asigna
 * {@link com.inklusport.subscriptions.config.SecuenciaIdCallback} antes de escribir.
 * Se mantienen ids numéricos porque las URLs de la API y los otros microservicios
 * ya referencian planes, suscripciones y pagos por número.
 */
public interface DocumentoSecuencial {

    Long getId();

    void setId(Long id);
}
