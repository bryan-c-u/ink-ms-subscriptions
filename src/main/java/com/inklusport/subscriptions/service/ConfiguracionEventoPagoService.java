package com.inklusport.suscripciones.service;

import com.inklusport.suscripciones.dto.ConfiguracionEventoPagoRequest;
import com.inklusport.suscripciones.dto.ConfiguracionEventoPagoResponse;
import com.inklusport.suscripciones.entity.ConfiguracionEventoPago;
import com.inklusport.suscripciones.enums.EstadoSuscripcion;
import com.inklusport.suscripciones.exception.ConfiguracionEventoPagoNotFoundException;
import com.inklusport.suscripciones.repository.ConfiguracionEventoPagoRepository;
import com.inklusport.suscripciones.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/** RF65: configuracion de un evento como gratuito o pago por parte del organizador. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionEventoPagoService {

    private final ConfiguracionEventoPagoRepository configuracionEventoPagoRepository;
    private final SuscripcionRepository suscripcionRepository;

    @Transactional
    public ConfiguracionEventoPagoResponse configurar(String organizadorId, ConfiguracionEventoPagoRequest request) {
        if (configuracionEventoPagoRepository.existsByEventoId(request.getEventoId())) {
            throw new IllegalStateException(
                    "El evento " + request.getEventoId() + " ya tiene una configuracion de pago; actualizala en su lugar");
        }
        if (Boolean.TRUE.equals(request.getEsPago()) && request.getValorInscripcion() == null) {
            throw new IllegalArgumentException("Debe indicar el valor de inscripcion para un evento de pago");
        }

        ConfiguracionEventoPago config = new ConfiguracionEventoPago();
        config.setEventoId(request.getEventoId());
        config.setOrganizadorId(organizadorId);
        config.setEsPago(Boolean.TRUE.equals(request.getEsPago()));
        config.setValorInscripcion(config.getEsPago() ? request.getValorInscripcion() : null);
        config.setPorcentajeComision(config.getEsPago() ? comisionVigente(organizadorId) : BigDecimal.ZERO);

        config = configuracionEventoPagoRepository.save(config);
        log.info("Evento {} configurado como {} por {}", request.getEventoId(),
                config.getEsPago() ? "pago" : "gratuito", organizadorId);
        return toResponse(config);
    }

    @Transactional
    public ConfiguracionEventoPagoResponse actualizar(String organizadorId, String eventoId,
                                                        ConfiguracionEventoPagoRequest request) {
        ConfiguracionEventoPago config = obtenerPropia(organizadorId, eventoId);
        if (Boolean.TRUE.equals(request.getEsPago()) && request.getValorInscripcion() == null) {
            throw new IllegalArgumentException("Debe indicar el valor de inscripcion para un evento de pago");
        }

        config.setEsPago(Boolean.TRUE.equals(request.getEsPago()));
        config.setValorInscripcion(config.getEsPago() ? request.getValorInscripcion() : null);
        if (config.getEsPago() && (config.getPorcentajeComision() == null
                || config.getPorcentajeComision().compareTo(BigDecimal.ZERO) == 0)) {
            config.setPorcentajeComision(comisionVigente(organizadorId));
        }

        config = configuracionEventoPagoRepository.save(config);
        return toResponse(config);
    }

    @Transactional(readOnly = true)
    public ConfiguracionEventoPagoResponse obtenerPorEvento(String eventoId) {
        return toResponse(obtenerEntidadPorEvento(eventoId));
    }

    @Transactional(readOnly = true)
    public ConfiguracionEventoPago obtenerEntidadPorEvento(String eventoId) {
        return configuracionEventoPagoRepository.findByEventoId(eventoId)
                .orElseThrow(() -> new ConfiguracionEventoPagoNotFoundException(eventoId));
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionEventoPagoResponse> listarPorOrganizador(String organizadorId) {
        return configuracionEventoPagoRepository.findByOrganizadorId(organizadorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ConfiguracionEventoPago obtenerPropia(String organizadorId, String eventoId) {
        ConfiguracionEventoPago config = obtenerEntidadPorEvento(eventoId);
        if (!config.getOrganizadorId().equals(organizadorId)) {
            throw new AccessDeniedException("No tienes acceso a la configuracion de este evento");
        }
        return config;
    }

    private BigDecimal comisionVigente(String organizadorId) {
        return suscripcionRepository
                .findFirstByOrganizadorIdAndEstadoOrderByFechaCreacionDesc(organizadorId, EstadoSuscripcion.ACTIVA)
                .map(s -> s.getPlan().getPorcentajeComision())
                .orElse(BigDecimal.ZERO);
    }

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
