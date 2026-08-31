package com.inklusport.suscripciones.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    private boolean correoDeshabilitado() {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("spring.mail.username (MAIL_USERNAME) no esta configurado; se omite el envio de correo");
            return true;
        }
        return false;
    }

    @Async
    public void enviarComprobantePago(String to, String numeroComprobante, String concepto,
                                       BigDecimal monto, File adjuntoPdf) {
        if (correoDeshabilitado()) {
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
            log.info("Comprobante {} enviado a: {}", numeroComprobante, to);
        } catch (MessagingException e) {
            log.error("Error al enviar comprobante {} a {}: {}", numeroComprobante, to, e.getMessage());
        }
    }

    @Async
    public void enviarAvisoVencimiento(String to, String planNombre, int diasRestantes) {
        if (correoDeshabilitado()) {
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
            log.info("Aviso de vencimiento enviado a: {}", to);
        } catch (MessagingException e) {
            log.error("Error al enviar aviso de vencimiento a {}: {}", to, e.getMessage());
        }
    }

    private String buildComprobanteContent(String numeroComprobante, String concepto, BigDecimal monto) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #1E3A8A;">InkluSport</h2>
                    <h3>Comprobante de pago</h3>
                    <p>Tu pago fue procesado exitosamente.</p>
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr><td style="padding: 6px; color:#666;">Concepto</td><td style="padding: 6px;"><strong>%s</strong></td></tr>
                        <tr><td style="padding: 6px; color:#666;">Monto</td><td style="padding: 6px;"><strong>$%s</strong></td></tr>
                        <tr><td style="padding: 6px; color:#666;">N&uacute;mero de comprobante</td><td style="padding: 6px;"><strong>%s</strong></td></tr>
                    </table>
                    <p>Adjuntamos el comprobante en formato PDF para tus registros.</p>
                    <hr>
                    <p style="font-size: 12px; color: #666;">InkluSport - Deporte para todos</p>
                </div>
            </body>
            </html>
            """.formatted(concepto, monto.toPlainString(), numeroComprobante);
    }

    private String buildAvisoVencimientoContent(String planNombre, int diasRestantes) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #1E3A8A;">InkluSport</h2>
                    <h3>Tu suscripcion esta por vencer</h3>
                    <p>Tu suscripcion al plan <strong>%s</strong> vence en <strong>%d dia(s)</strong>.</p>
                    <p>Renueva tu suscripcion para seguir disfrutando de todos los beneficios de tu plan sin interrupciones.</p>
                    <hr>
                    <p style="font-size: 12px; color: #666;">InkluSport - Deporte para todos</p>
                </div>
            </body>
            </html>
            """.formatted(planNombre, diasRestantes);
    }
}
