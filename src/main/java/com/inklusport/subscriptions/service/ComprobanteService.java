package com.inklusport.suscripciones.service;

import com.inklusport.suscripciones.entity.ComprobantePago;
import com.inklusport.suscripciones.entity.PagoEvento;
import com.inklusport.suscripciones.entity.PagoSuscripcion;
import com.inklusport.suscripciones.repository.ComprobantePagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Genera el comprobante en PDF (RF69) y su registro en BD, tanto para pagos de
 * inscripcion a eventos como para pagos de suscripcion de organizador.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprobanteService {

    private final ComprobantePagoRepository comprobantePagoRepository;

    @Value("${app.comprobantes.storage-path:./comprobantes}")
    private String storagePath;

    @Transactional
    public ComprobantePago generarComprobanteEvento(PagoEvento pagoEvento, String concepto) {
        return generar(pagoEvento, null, concepto, pagoEvento.getMonto(),
                pagoEvento.getReferenciaTransaccion(), pagoEvento.getFechaPago());
    }

    @Transactional
    public ComprobantePago generarComprobanteSuscripcion(PagoSuscripcion pagoSuscripcion, String concepto) {
        return generar(null, pagoSuscripcion, concepto, pagoSuscripcion.getMonto(),
                pagoSuscripcion.getReferenciaTransaccion(), pagoSuscripcion.getFechaPago());
    }

    /** Puede devolver null si el comprobante se registro pero su PDF no se pudo generar (ver generarPdf). */
    public File obtenerArchivo(ComprobantePago comprobante) {
        String ruta = comprobante.getUrlPdf();
        return (ruta != null && !ruta.isBlank()) ? new File(ruta) : null;
    }

    private ComprobantePago generar(PagoEvento pagoEvento, PagoSuscripcion pagoSuscripcion, String concepto,
                                     BigDecimal monto, String referencia, LocalDateTime fechaPago) {
        String numero = "CMP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        File pdf = generarPdf(numero, concepto, monto, referencia, fechaPago);

        ComprobantePago comprobante = new ComprobantePago();
        comprobante.setPagoEvento(pagoEvento);
        comprobante.setPagoSuscripcion(pagoSuscripcion);
        comprobante.setNumeroComprobante(numero);
        comprobante.setUrlPdf(pdf != null ? pdf.getPath() : null);
        return comprobantePagoRepository.save(comprobante);
    }

    private File generarPdf(String numero, String concepto, BigDecimal monto, String referencia,
                             LocalDateTime fechaPago) {
        try {
            File dir = new File(storagePath);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new java.io.IOException("No se pudo crear el directorio de comprobantes: " + storagePath);
            }
            File file = new File(dir, numero + ".pdf");

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("InkluSport", tituloFont));
            document.add(new Paragraph("Comprobante de pago", subtituloFont));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Numero de comprobante: " + numero, normalFont));
            document.add(new Paragraph("Concepto: " + concepto, normalFont));
            document.add(new Paragraph("Monto pagado: $" + monto.toPlainString(), normalFont));
            document.add(new Paragraph("Referencia de transaccion: " + (referencia != null ? referencia : "N/A"), normalFont));
            document.add(new Paragraph("Fecha de pago: " + fechaPago, normalFont));

            document.close();
            return file;
        } catch (Exception e) {
            // No se relanza: el pago ya fue aprobado por Mercado Pago y la suscripcion/inscripcion
            // ya se activo en la misma transaccion. Un fallo de disco al escribir el PDF no debe
            // revertir un cobro real; el comprobante queda registrado sin archivo y se puede regenerar.
            log.error("Error generando el PDF del comprobante {}: {}", numero, e.getMessage(), e);
            return null;
        }
    }
}
