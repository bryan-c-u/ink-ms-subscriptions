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
@Table(name = "pago_evento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Email del usuario (ver nota en Suscripcion.organizadorId): se usa como
    // destinatario directo del comprobante por correo (RF69) sin llamar a otro MS.
    @Column(name = "usuario_id", length = 100, nullable = false)
    private String usuarioId;

    // String (UUID) del evento, ver nota en ConfiguracionEventoPago.
    @Column(name = "evento_id", length = 36, nullable = false)
    private String eventoId;

    @Column(name = "monto", precision = 10, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(name = "referencia_transaccion", length = 150)
    private String referenciaTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPago estado;

    // Bloqueo optimista frente a notificaciones duplicadas/concurrentes de Mercado Pago
    // (ver nota en PagoSuscripcion.version).
    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;
}
