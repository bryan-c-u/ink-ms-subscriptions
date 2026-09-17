package com.inklusport.subscriptions.entity;

import com.inklusport.subscriptions.enums.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RF61: historial de altas, renovaciones, cambios de plan y estados.
 * Guarda {@code organizador_id} además de la suscripción porque en Mongo no hay JOIN y el
 * panel de administración consulta el historial por organizador.
 */
@Document(collection = "historial_suscripcion")
@CompoundIndex(name = "idx_historial_suscripcion", def = "{'suscripcion_id': 1, 'fecha_movimiento': -1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialSuscripcion implements DocumentoSecuencial {

    @Id
    private Long id;

    @Field("suscripcion_id")
    private Long suscripcionId;

    @Indexed
    @Field("organizador_id")
    private String organizadorId;

    @Field("tipo_movimiento")
    private TipoMovimiento tipoMovimiento;

    @Field("plan_anterior_id")
    private Long planAnteriorId;

    @Field("plan_anterior_nombre")
    private String planAnteriorNombre;

    @Field("plan_nuevo_id")
    private Long planNuevoId;

    @Field("plan_nuevo_nombre")
    private String planNuevoNombre;

    @Field("estado_anterior")
    private String estadoAnterior;

    @Field("estado_nuevo")
    private String estadoNuevo;

    @Field("fecha_fin_anterior")
    private LocalDate fechaFinAnterior;

    @Field("fecha_fin_nueva")
    private LocalDate fechaFinNueva;

    private BigDecimal monto;

    /** UUID del actor; {@code null} = sistema. */
    @Field("realizado_por")
    private String realizadoPor;

    private String notas;

    @CreatedDate
    @Field("fecha_movimiento")
    private LocalDateTime fechaMovimiento;
}
