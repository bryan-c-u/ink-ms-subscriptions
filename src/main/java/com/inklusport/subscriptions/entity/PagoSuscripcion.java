package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.enums.TipoPagoSuscripcion;
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
 * RF56 / RF59 — pagos de planes de organizador.
 * {@code organizador_id} está duplicado desde la suscripción para poder validar la
 * propiedad del pago sin una segunda lectura.
 */
@Document(collection = "pago_suscripcion")
@CompoundIndex(name = "idx_pago_suscripcion", def = "{'suscripcion_id': 1, 'estado': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoSuscripcion implements DocumentoSecuencial {

    @Id
    private Long id;

    @Field("suscripcion_id")
    private Long suscripcionId;

    @Indexed
    @Field("organizador_id")
    private String organizadorId;

    @Field("transaccion_id")
    private Long transaccionId;

    private TipoPagoSuscripcion tipo = TipoPagoSuscripcion.NUEVA;

    private BigDecimal monto;

    private String moneda = "COP";

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
