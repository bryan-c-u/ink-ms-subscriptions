package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RF55 / RF57 / RF66 — inscripción pagada a un evento.
 * Si el evento está configurado como de pago, el atleta paga la inscripción
 * (incluida la primera vez); no hay alta gratuita previa al checkout.
 * {@code inscripcion_id} apunta a {@code sports_events_ms.event_registration}
 * (referencia lógica entre microservicios, sin integridad declarada).
 */
@Document(collection = "pago_evento")
@CompoundIndex(name = "idx_pago_evento_usuario", def = "{'usuario_id': 1, 'fecha_pago': -1}")
@CompoundIndex(name = "idx_pago_evento_evento", def = "{'evento_id': 1, 'estado': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoEvento implements DocumentoSecuencial {

    @Id
    private Long id;

    @Field("usuario_id")
    private String usuarioId;

    @Field("evento_id")
    private String eventoId;

    @Indexed(name = "idx_pago_evento_organizador")
    @Field("organizador_id")
    private String organizadorId;

    @Field("inscripcion_id")
    private String inscripcionId;

    @Field("transaccion_id")
    private Long transaccionId;

    /** Snapshot para el historial del usuario (RF66). */
    @Field("nombre_evento")
    private String nombreEvento;

    private BigDecimal monto;

    private String moneda = "COP";

    @Field("porcentaje_comision")
    private BigDecimal porcentajeComision = BigDecimal.ZERO;

    @Field("comision_plataforma")
    private BigDecimal comisionPlataforma = BigDecimal.ZERO;

    @Field("monto_neto_organizador")
    private BigDecimal montoNetoOrganizador = BigDecimal.ZERO;

    @Field("metodo_pago")
    private String metodoPago;

    @Indexed
    @Field("referencia_transaccion")
    private String referenciaTransaccion;

    private EstadoPago estado = EstadoPago.PENDIENTE;

    @CreatedDate
    @Field("fecha_pago")
    private LocalDateTime fechaPago;
}
