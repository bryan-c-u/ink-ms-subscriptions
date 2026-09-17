package com.inklusport.subscriptions.dto;

import com.inklusport.subscriptions.enums.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialSuscripcionResponse {
    private Long id;
    private Long suscripcionId;
    private TipoMovimiento tipoMovimiento;
    private Long planAnteriorId;
    private String planAnteriorNombre;
    private Long planNuevoId;
    private String planNuevoNombre;
    private String estadoAnterior;
    private String estadoNuevo;
    /** Motivo u observación opcional, solo presente en cambios de estado aplicados por un admin. */
    private String notas;
    /** UUID del admin que aplicó el cambio, o {@code null} si fue el propio organizador o el sistema. */
    private String realizadoPor;
    private String realizadoPorEmail;
    private LocalDateTime fechaMovimiento;
}
