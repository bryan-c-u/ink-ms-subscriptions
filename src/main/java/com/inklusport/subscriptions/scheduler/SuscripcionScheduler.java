package com.inklusport.subscriptions.scheduler;

import com.inklusport.subscriptions.entity.NotificacionVencimiento;
import com.inklusport.subscriptions.entity.Suscripcion;
import com.inklusport.subscriptions.enums.EstadoSuscripcion;
import com.inklusport.subscriptions.enums.TipoMovimiento;
import com.inklusport.subscriptions.entity.HistorialSuscripcion;
import com.inklusport.subscriptions.repository.HistorialSuscripcionRepository;
import com.inklusport.subscriptions.repository.NotificacionVencimientoRepository;
import com.inklusport.subscriptions.repository.SuscripcionRepository;
import com.inklusport.subscriptions.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuscripcionScheduler {

    private final SuscripcionRepository suscripcionRepository;
    private final HistorialSuscripcionRepository historialSuscripcionRepository;
    private final NotificacionVencimientoRepository notificacionVencimientoRepository;
    private final EmailService emailService;

    @Value("${app.suscripciones.dias-aviso-vencimiento:7}")
    private String diasAvisoVencimiento;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void marcarSuscripcionesVencidas() {
        List<Suscripcion> vencidas = suscripcionRepository
                .findByEstadoAndFechaFinBefore(EstadoSuscripcion.ACTIVA, LocalDate.now());
        for (Suscripcion s : vencidas) {
            s.setEstado(EstadoSuscripcion.VENCIDA);
            HistorialSuscripcion h = new HistorialSuscripcion();
            h.setSuscripcion(s);
            h.setTipoMovimiento(TipoMovimiento.VENCIMIENTO);
            h.setPlanNuevoId(s.getPlan().getId());
            h.setEstadoAnterior(EstadoSuscripcion.ACTIVA.name());
            h.setEstadoNuevo(EstadoSuscripcion.VENCIDA.name());
            historialSuscripcionRepository.save(h);
        }
        suscripcionRepository.saveAll(vencidas);
        if (!vencidas.isEmpty()) {
            log.info("{} suscripcion(es) marcada(s) como VENCIDA", vencidas.size());
        }
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void avisarVencimientoProximo() {
        LocalDate hoy = LocalDate.now();
        List<Integer> dias = Arrays.stream(diasAvisoVencimiento.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(Integer::parseInt)
                .toList();
        if (dias.isEmpty()) {
            dias = List.of(7);
        }
        int max = dias.stream().mapToInt(Integer::intValue).max().orElse(7);
        List<Suscripcion> porVencer = suscripcionRepository
                .findByEstadoAndFechaFinBetween(EstadoSuscripcion.ACTIVA, hoy, hoy.plusDays(max));

        for (Suscripcion suscripcion : porVencer) {
            long diasRestantes = ChronoUnit.DAYS.between(hoy, suscripcion.getFechaFin());
            if (!dias.contains((int) diasRestantes)) {
                continue;
            }
            var yaEnviada = notificacionVencimientoRepository.findBySuscripcionIdAndDiasAntesAndFechaProgramada(
                    suscripcion.getId(), (int) diasRestantes, hoy);
            if (yaEnviada.isPresent()) {
                continue;
            }
            NotificacionVencimiento n = new NotificacionVencimiento();
            n.setSuscripcion(suscripcion);
            n.setDiasAntes((int) diasRestantes);
            n.setFechaProgramada(hoy);
            n.setDestinatario(suscripcion.getOrganizadorId());
            try {
                emailService.enviarAvisoVencimiento(suscripcion.getOrganizadorId(),
                        suscripcion.getPlan().getNombre(), (int) diasRestantes);
                n.setEstado("ENVIADA");
                n.setFechaEnvio(LocalDateTime.now());
            } catch (Exception e) {
                n.setEstado("FALLIDA");
                n.setErrorEnvio(e.getMessage());
            }
            notificacionVencimientoRepository.save(n);
        }
    }

    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void reiniciarContadorEventosMensual() {
        suscripcionRepository.reiniciarContadorEventosMensual(LocalDate.now().withDayOfMonth(1));
        log.info("Contador mensual de eventos reiniciado");
    }
}
