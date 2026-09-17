package com.inklusport.subscriptions.repository;

import com.inklusport.subscriptions.entity.PagoEvento;
import com.inklusport.subscriptions.enums.EstadoPago;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoEventoRepository extends MongoRepository<PagoEvento, Long> {

    List<PagoEvento> findByUsuarioIdOrderByFechaPagoDesc(String usuarioId);

    List<PagoEvento> findByOrganizadorIdOrderByFechaPagoDesc(String organizadorId);

    Optional<PagoEvento> findByReferenciaTransaccion(String referenciaTransaccion);

    Optional<PagoEvento> findByUsuarioIdAndEventoIdAndEstado(String usuarioId, String eventoId, EstadoPago estado);

    List<PagoEvento> findByUsuarioIdAndEventoIdAndEstadoOrderByIdDesc(
            String usuarioId, String eventoId, EstadoPago estado);

    /** RF62: base del reporte global; el agrupado por evento se hace en el servicio. */
    List<PagoEvento> findByEstadoAndFechaPagoBetween(EstadoPago estado, LocalDateTime desde, LocalDateTime hasta);

    /** RF62: base del reporte de un organizador. */
    List<PagoEvento> findByOrganizadorIdAndEstadoAndFechaPagoBetween(
            String organizadorId, EstadoPago estado, LocalDateTime desde, LocalDateTime hasta);
}
