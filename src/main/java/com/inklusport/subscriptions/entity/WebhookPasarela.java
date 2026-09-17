package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.Pasarela;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/** RF68: bitácora de notificaciones IPN / webhooks (idempotencia). */
@Document(collection = "webhook_pasarela")
@CompoundIndex(name = "idx_webhook_externo", def = "{'pasarela': 1, 'id_externo': 1}")
@CompoundIndex(name = "idx_webhook_pendiente", def = "{'procesado': 1, 'fecha_recepcion': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPasarela implements DocumentoSecuencial {

    @Id
    private Long id;

    private Pasarela pasarela = Pasarela.MERCADOPAGO;

    /** payment, merchant_order, plan, subscription, ... */
    @Field("tipo_notificacion")
    private String tipoNotificacion;

    @Field("id_externo")
    private String idExterno;

    @Field("transaccion_id")
    private Long transaccionId;

    private String payload;

    @Field("firma_recibida")
    private String firmaRecibida;

    private Boolean procesado = false;

    private String resultado;

    @CreatedDate
    @Field("fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @Field("fecha_proceso")
    private LocalDateTime fechaProceso;
}
