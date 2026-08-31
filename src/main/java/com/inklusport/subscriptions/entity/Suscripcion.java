package com.inklusport.suscripciones.entity;

import com.inklusport.suscripciones.enums.EstadoSuscripcion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "suscripcion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guarda el email del organizador (principal autenticado via JWT en todo el
    // sistema, ver JwtAuthenticationFilter), no un id numerico: es el unico
    // identificador estable disponible sin llamar a ink-ms-users. String en vez
    // de BIGINT porque el script SQL original asumia ids numericos que no existen.
    @Column(name = "organizador_id", length = 100, nullable = false)
    private String organizadorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoSuscripcion estado;

    @Column(name = "eventos_creados_mes")
    private Integer eventosCreadosMes = 0;

    @Column(name = "renovacion_automatica")
    private Boolean renovacionAutomatica = false;

    @CreationTimestamp
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}
