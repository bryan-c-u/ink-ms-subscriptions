package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.TipoComprobante;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobante_pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprobantePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_evento_id")
    private PagoEvento pagoEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_suscripcion_id")
    private PagoSuscripcion pagoSuscripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id")
    private TransaccionPasarela transaccion;

    @Column(name = "numero_comprobante", nullable = false, unique = true, length = 100)
    private String numeroComprobante;

    @Column(name = "numero_transaccion", length = 150)
    private String numeroTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoComprobante tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda = "COP";

    @Column(name = "detalle_evento", length = 255)
    private String detalleEvento;

    @Column(name = "email_destino", length = 150)
    private String emailDestino;

    @Column(name = "email_enviado", nullable = false)
    private Boolean emailEnviado = false;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "error_envio", length = 500)
    private String errorEnvio;

    @Column(name = "url_pdf", length = 500)
    private String urlPdf;

    @CreationTimestamp
    @Column(name = "fecha_generacion", nullable = false, updatable = false)
    private LocalDateTime fechaGeneracion;
}
