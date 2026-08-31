package com.inklusport.suscripciones.dto;

import com.inklusport.suscripciones.enums.TipoMovimiento;
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
    private LocalDateTime fechaMovimiento;
}
