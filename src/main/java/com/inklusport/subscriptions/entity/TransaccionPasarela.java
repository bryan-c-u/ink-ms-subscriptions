package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.Pasarela;
import com.inklusport.subscriptions.enums.TipoTransaccion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaccion_pasarela")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionPasarela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Pasarela pasarela = Pasarela.MERCADOPAGO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoTransaccion tipo;

    @Column(name = "preferencia_id", length = 100)
    private String preferenciaId;

    @Column(name = "pago_externo_id", length = 100)
    private String pagoExternoId;

    @Column(name = "orden_externa_id", length = 100)
    private String ordenExternaId;

    @Column(name = "referencia_externa", length = 150)
    private String referenciaExterna;

    @Column(name = "estado_pasarela", length = 50)
    private String estadoPasarela;

    @Column(name = "detalle_estado", length = 100)
    private String detalleEstado;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(name = "tipo_pago", length = 50)
    private String tipoPago;

    @Column(nullable = false, length = 3)
    private String moneda = "COP";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "url_checkout", length = 500)
    private String urlCheckout;

    @Column(name = "payload_creacion", columnDefinition = "json")
    private String payloadCreacion;

    @Column(name = "payload_webhook", columnDefinition = "json")
    private String payloadWebhook;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
