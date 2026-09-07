package com.inklusport.subscriptions.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;

/**
 * Servicio de envío asíncrono de correos transaccionales.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail-enabled:false}")
    private boolean mailEnabled;

    /**
     * Indica si el envío de correo está deshabilitado.
     *
     * @return {@code true} si no se debe enviar correo
     */
    private boolean correoDeshabilitado() {
        if (!mailEnabled || fromEmail == null || fromEmail.isBlank()) {
            log.debug("Correo deshabilitado; se omite el envio");
            return true;
        }
        return false;
    }

    /**
     * Envía el comprobante de pago al destinatario, con PDF adjunto si existe.
     *
     * @param to                 correo destino
     * @param numeroComprobante  número del comprobante
     * @param concepto           concepto del pago
     * @param monto              monto pagado
     * @param adjuntoPdf         PDF del comprobante, o {@code null}
     */
    @Async
    public void enviarComprobantePago(String to, String numeroComprobante, String concepto,
                                      BigDecimal monto, File adjuntoPdf) {
        if (correoDeshabilitado() || to == null || !to.contains("@")) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, adjuntoPdf != null, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Comprobante de pago - InkluSport");
            helper.setText(buildComprobanteContent(numeroComprobante, concepto, monto), true);
            if (adjuntoPdf != null && adjuntoPdf.exists()) {
                helper.addAttachment("comprobante-" + numeroComprobante + ".pdf", adjuntoPdf);
            }
            mailSender.send(message);
            log.info("Comprobante {} enviado a {}", numeroComprobante, to);
        } catch (MessagingException e) {
            log.error("Error al enviar comprobante {} a {}: {}", numeroComprobante, to, e.getMessage());
        }
    }

    /**
     * Envía un aviso de vencimiento próximo de la suscripción.
     *
     * @param to            correo destino
     * @param planNombre    nombre del plan
     * @param diasRestantes días restantes de vigencia
     */
    @Async
    public void enviarAvisoVencimiento(String to, String planNombre, int diasRestantes) {
        if (correoDeshabilitado() || to == null || !to.contains("@")) {
            log.info("Aviso de vencimiento del plan {} ({} dias) para {}", planNombre, diasRestantes, to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Tu suscripcion esta por vencer - InkluSport");
            helper.setText(buildAvisoVencimientoContent(planNombre, diasRestantes), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Error al enviar aviso de vencimiento a {}: {}", to, e.getMessage());
        }
    }

    /**
     * Construye el HTML del correo de comprobante.
     *
     * @param numeroComprobante número del comprobante
     * @param concepto          concepto del pago
     * @param monto             monto pagado
     * @return cuerpo HTML
     */
    private String buildComprobanteContent(String numeroComprobante, String concepto, BigDecimal monto) {
        return """
            <html><body style="font-family: Arial, sans-serif;">
            <h2>InkluSport</h2>
            <p>Tu pago fue procesado exitosamente.</p>
            <p>Concepto: <strong>%s</strong></p>
            <p>Monto: <strong>$%s</strong></p>
            <p>Comprobante: <strong>%s</strong></p>
            </body></html>
            """.formatted(concepto, monto.toPlainString(), numeroComprobante);
    }

    /**
     * Construye el HTML del aviso de vencimiento.
     *
     * @param planNombre    nombre del plan
     * @param diasRestantes días restantes de vigencia
     * @return cuerpo HTML
     */
    private String buildAvisoVencimientoContent(String planNombre, int diasRestantes) {
        return """
            <html><body style="font-family: Arial, sans-serif;">
            <h2>InkluSport</h2>
            <p>Tu suscripcion al plan <strong>%s</strong> vence en <strong>%d dia(s)</strong>.</p>
            </body></html>
            """.formatted(planNombre, diasRestantes);
    }
}
