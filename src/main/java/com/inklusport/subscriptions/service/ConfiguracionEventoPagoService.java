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

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionEventoPagoService {

    private final ConfiguracionEventoPagoRepository configuracionEventoPagoRepository;
    private final SuscripcionRepository suscripcionRepository;

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
                .map(s -> s.getPorcentajeComisionAplicado() != null
                        ? s.getPorcentajeComisionAplicado()
                        : s.getPlan().getPorcentajeComision())
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
