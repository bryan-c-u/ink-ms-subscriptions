package com.inklusport.suscripciones.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(name = "numero_comprobante", length = 100, unique = true, nullable = false)
    private String numeroComprobante;

    @Column(name = "url_pdf", length = 500)
    private String urlPdf;

    @CreationTimestamp
    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;
}
