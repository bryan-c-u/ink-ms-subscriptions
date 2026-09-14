package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.Pasarela;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_pasarela")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPasarela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Pasarela pasarela = Pasarela.MERCADOPAGO;

    @Column(name = "tipo_notificacion", nullable = false, length = 80)
    private String tipoNotificacion;

    @Column(name = "id_externo", length = 100)
    private String idExterno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id")
    private TransaccionPasarela transaccion;

    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "firma_recibida", length = 255)
    private String firmaRecibida;

    @Column(nullable = false)
    private Boolean procesado = false;

    @Column(length = 500)
    private String resultado;

    @CreationTimestamp
    @Column(name = "fecha_recepcion", nullable = false, updatable = false)
    private LocalDateTime fechaRecepcion;

    @Column(name = "fecha_proceso")
    private LocalDateTime fechaProceso;
}
