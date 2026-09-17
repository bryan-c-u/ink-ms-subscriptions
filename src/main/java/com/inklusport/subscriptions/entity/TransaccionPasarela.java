package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.Pasarela;
import com.inklusport.subscriptions.enums.TipoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RF68 — transacciones con la pasarela (Mercado Pago).
 * Un documento por intento de cobro: inscripción a evento o plan de organizador.
 */
@Document(collection = "transaccion_pasarela")
// Checkout con tarjeta (RF70) no crea Preference de Checkout Pro: preferencia_id queda
 // null. Un unique+sparse indexa ese null y el 2.º cobro explota con E11000.
// Solo unicidad cuando hay preference id real (string).
@CompoundIndex(
        name = "uk_preferencia",
        def = "{'pasarela': 1, 'preferencia_id': 1}",
        unique = true,
        partialFilter = "{ 'preferencia_id': { $type: 'string' } }"
)
@CompoundIndex(
        name = "uk_pago_externo",
        def = "{'pasarela': 1, 'pago_externo_id': 1}",
        unique = true,
        partialFilter = "{ 'pago_externo_id': { $type: 'string' } }"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionPasarela implements DocumentoSecuencial {

    @Id
    private Long id;

    private Pasarela pasarela = Pasarela.MERCADOPAGO;

    private TipoTransaccion tipo;

    /** Preference.id de Checkout Pro. */
    @Field("preferencia_id")
    private String preferenciaId;

    /** Payment.id de Mercado Pago. */
    @Field("pago_externo_id")
    private String pagoExternoId;

    /** Merchant order id. */
    @Field("orden_externa_id")
    private String ordenExternaId;

    @Indexed
    @Field("referencia_externa")
    private String referenciaExterna;

    /** approved, pending, rejected, refunded, cancelled, ... */
    @Indexed
    @Field("estado_pasarela")
    private String estadoPasarela;

    /** status_detail de Mercado Pago. */
    @Field("detalle_estado")
    private String detalleEstado;

    /** visa, master, pse, account_money, ... */
    @Field("metodo_pago")
    private String metodoPago;

    /** credit_card, debit_card, bank_transfer, ticket, ... */
    @Field("tipo_pago")
    private String tipoPago;

    private String moneda = "COP";

    private BigDecimal monto;

    /** init_point / sandbox_init_point. */
    @Field("url_checkout")
    private String urlCheckout;

    @Field("payload_creacion")
    private String payloadCreacion;

    @Field("payload_webhook")
    private String payloadWebhook;

    @CreatedDate
    @Field("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Field("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
