package com.inklusport.subscriptions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_evento_pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionEventoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, unique = true, length = 36)
    private String eventoId;

    @Column(name = "organizador_id", nullable = false, length = 100)
    private String organizadorId;

    @Column(name = "es_pago", nullable = false)
    private Boolean esPago = false;

    @Column(name = "valor_inscripcion", precision = 12, scale = 2)
    private BigDecimal valorInscripcion;

    @Column(nullable = false, length = 3)
    private String moneda = "COP";

    @Column(name = "porcentaje_comision", precision = 5, scale = 2)
    private BigDecimal porcentajeComision;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
