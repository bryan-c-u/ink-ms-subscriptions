package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.TipoComprobante;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** RF67 — comprobante de pago e inscripción (PDF + envío por correo). */
@Document(collection = "comprobante_pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprobantePago implements DocumentoSecuencial {

    @Id
    private Long id;

    @Indexed(sparse = true)
    @Field("pago_evento_id")
    private Long pagoEventoId;

    @Indexed(sparse = true)
    @Field("pago_suscripcion_id")
    private Long pagoSuscripcionId;

    @Field("transaccion_id")
    private Long transaccionId;

    @Indexed(unique = true)
    @Field("numero_comprobante")
    private String numeroComprobante;

    @Field("numero_transaccion")
    private String numeroTransaccion;

    private TipoComprobante tipo;

    private BigDecimal monto;

    private String moneda = "COP";

    @Field("detalle_evento")
    private String detalleEvento;

    @Field("email_destino")
    private String emailDestino;

    @Field("email_enviado")
    private Boolean emailEnviado = false;

    @Field("fecha_envio")
    private LocalDateTime fechaEnvio;

    @Field("error_envio")
    private String errorEnvio;

    @Field("url_pdf")
    private String urlPdf;

    @CreatedDate
    @Field("fecha_generacion")
    private LocalDateTime fechaGeneracion;
}
