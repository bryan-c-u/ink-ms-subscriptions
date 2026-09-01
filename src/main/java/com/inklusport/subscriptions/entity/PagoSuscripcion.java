package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.EstadoPago;
import com.inklusport.subscriptions.enums.TipoPagoSuscripcion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago_suscripcion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id")
    private TransaccionPasarela transaccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPagoSuscripcion tipo = TipoPagoSuscripcion.NUEVA;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda = "COP";

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(name = "referencia_transaccion", length = 150)
    private String referenciaTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @CreationTimestamp
    @Column(name = "fecha_pago", nullable = false, updatable = false)
    private LocalDateTime fechaPago;
}
