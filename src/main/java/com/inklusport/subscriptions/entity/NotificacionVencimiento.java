package com.inklusport.subscriptions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion_vencimiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionVencimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @Column(name = "dias_antes", nullable = false)
    private Integer diasAntes;

    @Column(nullable = false, length = 20)
    private String canal = "EMAIL";

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(length = 150)
    private String destinatario;

    @Column(name = "error_envio", length = 500)
    private String errorEnvio;
}
