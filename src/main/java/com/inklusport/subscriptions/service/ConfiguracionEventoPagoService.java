package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.dto.ConfiguracionEventoPagoRequest;
import com.inklusport.subscriptions.dto.ConfiguracionEventoPagoResponse;
import com.inklusport.subscriptions.entity.ConfiguracionEventoPago;
import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import com.inklusport.subscriptions.exception.ConfiguracionEventoPagoNotFoundException;
import com.inklusport.subscriptions.repository.ConfiguracionEventoPagoRepository;
import com.inklusport.subscriptions.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de configuración de cobro e inscripción de eventos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionEventoPagoService {

    private final ConfiguracionEventoPagoRepository configuracionEventoPagoRepository;
    private final SuscripcionRepository suscripcionRepository;

    /**
     * Crea la configuración de pago de un evento.
     *
     * @param organizadorId identificador del organizador
     * @param request       datos de cobro e inscripción
     * @return configuración creada
     */
    @Transactional
    public ConfiguracionEventoPagoResponse configurar(String organizadorId, ConfiguracionEventoPagoRequest request) {
        if (configuracionEventoPagoRepository.existsByEventoId(request.getEventoId())) {
            throw new IllegalStateException("El evento " + request.getEventoId() + " ya tiene configuracion de pago");
        }
        if (Boolean.TRUE.equals(request.getEsPago()) && request.getValorInscripcion() == null) {
            throw new IllegalArgumentException("Debe indicar el valor de inscripcion para un evento de pago");
        }

        ConfiguracionEventoPago config = new ConfiguracionEventoPago();
        config.setEventoId(request.getEventoId());
        config.setOrganizadorId(organizadorId);
        config.setEsPago(Boolean.TRUE.equals(request.getEsPago()));
        config.setValorInscripcion(config.getEsPago() ? request.getValorInscripcion() : null);
        config.setMoneda("COP");
        config.setPorcentajeComision(config.getEsPago() ? comisionVigente(organizadorId) : BigDecimal.ZERO);
        config = configuracionEventoPagoRepository.save(config);
        return toResponse(config);
    }

    /**
     * Actualiza la configuración de pago de un evento propio.
     *
     * @param organizadorId identificador del organizador
     * @param eventoId      identificador del evento
     * @param request       nuevos datos de cobro
     * @return configuración actualizada
     */
    @Transactional
    public ConfiguracionEventoPagoResponse actualizar(String organizadorId, String eventoId,
                                                      ConfiguracionEventoPagoRequest request) {
        ConfiguracionEventoPago config = obtenerPropia(organizadorId, eventoId);
        if (Boolean.TRUE.equals(request.getEsPago()) && request.getValorInscripcion() == null) {
            throw new IllegalArgumentException("Debe indicar el valor de inscripcion para un evento de pago");
        }
        config.setEsPago(Boolean.TRUE.equals(request.getEsPago()));
        config.setValorInscripcion(config.getEsPago() ? request.getValorInscripcion() : null);
        if (config.getEsPago()) {
            config.setPorcentajeComision(comisionVigente(organizadorId));
        }
        return toResponse(configuracionEventoPagoRepository.save(config));
    }

    /**
     * Obtiene la configuración de pago de un evento.
     *
     * @param eventoId identificador del evento
     * @return configuración del evento
     */
    @Transactional(readOnly = true)
    public ConfiguracionEventoPagoResponse obtenerPorEvento(String eventoId) {
        return toResponse(obtenerEntidadPorEvento(eventoId));
    }

    /**
     * Obtiene la entidad de configuración de un evento.
     *
     * @param eventoId identificador del evento
     * @return entidad persistida
     */
    @Transactional(readOnly = true)
    public ConfiguracionEventoPago obtenerEntidadPorEvento(String eventoId) {
        return configuracionEventoPagoRepository.findByEventoId(eventoId)
                .orElseThrow(() -> new ConfiguracionEventoPagoNotFoundException(eventoId));
    }

    /**
     * Lista las configuraciones de pago de un organizador.
     *
     * @param organizadorId identificador del organizador
     * @return configuraciones del organizador
     */
    @Transactional(readOnly = true)
    public List<ConfiguracionEventoPagoResponse> listarPorOrganizador(String organizadorId) {
        return configuracionEventoPagoRepository.findByOrganizadorId(organizadorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene la configuración de un evento y valida que pertenezca al organizador.
     *
     * @param organizadorId identificador del organizador
     * @param eventoId      identificador del evento
     * @return configuración del organizador
     */
    private ConfiguracionEventoPago obtenerPropia(String organizadorId, String eventoId) {
        ConfiguracionEventoPago config = obtenerEntidadPorEvento(eventoId);
        if (!config.getOrganizadorId().equals(organizadorId)) {
            throw new AccessDeniedException("No tienes acceso a la configuracion de este evento");
        }
        return config;
    }

    /**
     * Calcula la comisión vigente según la suscripción activa del organizador.
     *
     * @param organizadorId identificador del organizador
     * @return porcentaje de comisión o cero si no hay suscripción activa
     */
    private BigDecimal comisionVigente(String organizadorId) {
        return suscripcionRepository
                .findFirstByOrganizadorIdAndEstadoOrderByFechaCreacionDesc(organizadorId, EstadoSuscripcion.ACTIVA)
                .map(s -> s.getPorcentajeComisionAplicado() != null
                        ? s.getPorcentajeComisionAplicado()
                        : s.getPlan().getPorcentajeComision())
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Convierte la entidad a DTO de respuesta.
     *
     * @param config configuración persistida
     * @return DTO de respuesta
     */
    private ConfiguracionEventoPagoResponse toResponse(ConfiguracionEventoPago config) {
        return ConfiguracionEventoPagoResponse.builder()
                .id(config.getId())
                .eventoId(config.getEventoId())
                .organizadorId(config.getOrganizadorId())
                .esPago(config.getEsPago())
                .valorInscripcion(config.getValorInscripcion())
                .porcentajeComision(config.getPorcentajeComision())
                .fechaCreacion(config.getFechaCreacion())
                .build();
    }
}
