package com.inklusport.suscripciones.entity;

import com.inklusport.suscripciones.enums.EstadoPago;
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

    @Column(name = "monto", precision = 10, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(name = "referencia_transaccion", length = 150)
    private String referenciaTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPago estado;

    // Bloqueo optimista: si Mercado Pago envia dos notificaciones del mismo pago casi
    // simultaneas, solo una podra confirmarlo; la otra falla con OptimisticLockException
    // (el webhook la registra y responde 200 igual). Evita doble activacion / doble comprobante.
    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;
}
