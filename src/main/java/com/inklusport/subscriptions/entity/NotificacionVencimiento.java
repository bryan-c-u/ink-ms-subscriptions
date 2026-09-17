package com.inklusport.subscriptions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** RF60 — avisos de vencimiento generados por el job del sistema. */
@Document(collection = "notificacion_vencimiento")
@CompoundIndex(name = "uk_notif_ciclo",
        def = "{'suscripcion_id': 1, 'dias_antes': 1, 'fecha_programada': 1}", unique = true)
@CompoundIndex(name = "idx_notif_pendiente", def = "{'estado': 1, 'fecha_programada': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionVencimiento implements DocumentoSecuencial {

    @Id
    private Long id;

    @Field("suscripcion_id")
    private Long suscripcionId;

    @Field("dias_antes")
    private Integer diasAntes;

    private String canal = "EMAIL";

    private String estado = "PENDIENTE";

    @Field("fecha_programada")
    private LocalDate fechaProgramada;

    @Field("fecha_envio")
    private LocalDateTime fechaEnvio;

    private String destinatario;

    @Field("error_envio")
    private String errorEnvio;
}
