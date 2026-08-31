package com.inklusport.suscripciones.scheduler;

import com.inklusport.suscripciones.entity.Suscripcion;
import com.inklusport.suscripciones.enums.EstadoSuscripcion;
import com.inklusport.suscripciones.repository.SuscripcionRepository;
import com.inklusport.suscripciones.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** RF59 (expiracion), RF60 (reinicio mensual de limites) y RF62 (aviso de vencimiento). */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuscripcionScheduler {

    private final SuscripcionRepository suscripcionRepository;
    private final EmailService emailService;

    @Value("${app.suscripciones.dias-aviso-vencimiento:5}")
    private int diasAvisoVencimiento;

    /** Todos los dias a la 1:00 am: marca como VENCIDA toda suscripcion activa cuya fecha_fin ya paso. */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void marcarSuscripcionesVencidas() {
        List<Suscripcion> vencidas = suscripcionRepository
                .findByEstadoAndFechaFinBefore(EstadoSuscripcion.ACTIVA, LocalDate.now());
        vencidas.forEach(s -> s.setEstado(EstadoSuscripcion.VENCIDA));
        suscripcionRepository.saveAll(vencidas);
        if (!vencidas.isEmpty()) {
            log.info("{} suscripcion(es) marcada(s) como VENCIDA", vencidas.size());
        }
    }

    /** Todos los dias a las 8:00 am: avisa por correo a quienes esten por vencer (RF62). */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void avisarVencimientoProximo() {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(diasAvisoVencimiento);
        List<Suscripcion> porVencer = suscripcionRepository
                .findByEstadoAndFechaFinBetween(EstadoSuscripcion.ACTIVA, hoy, limite);

        for (Suscripcion suscripcion : porVencer) {
            long diasRestantes = ChronoUnit.DAYS.between(hoy, suscripcion.getFechaFin());
            emailService.enviarAvisoVencimiento(suscripcion.getOrganizadorId(),
                    suscripcion.getPlan().getNombre(), (int) diasRestantes);
        }
    }

    /** El primer dia de cada mes a medianoche: reinicia el contador de eventos por mes (RF60). */
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void reiniciarContadorEventosMensual() {
        suscripcionRepository.reiniciarContadorEventosMensual();
        log.info("Contador mensual de eventos creados reiniciado para las suscripciones activas");
    }
}
