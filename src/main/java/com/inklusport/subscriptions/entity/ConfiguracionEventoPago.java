package com.inklusport.suscripciones.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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

    // evento_id: String (UUID) en vez de BIGINT, se alinea con el futuro id de
    // ink-ms-eventos. Unicidad de evento_id se valida en el service (no en la BD)
    // para respetar el resto del script SQL original tal cual fue entregado.
    @Column(name = "evento_id", length = 36, nullable = false)
    private String eventoId;

    // Email del organizador (ver nota en Suscripcion.organizadorId).
    @Column(name = "organizador_id", length = 100, nullable = false)
    private String organizadorId;

    @Column(name = "es_pago")
    private Boolean esPago = false;

    @Column(name = "valor_inscripcion", precision = 10, scale = 2)
    private BigDecimal valorInscripcion;

    @Column(name = "porcentaje_comision", precision = 5, scale = 2)
    private BigDecimal porcentajeComision;

    @CreationTimestamp
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}
