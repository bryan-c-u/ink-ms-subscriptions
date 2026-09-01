package com.inklusport.subscriptions.service;

import com.inklusport.subscriptions.entity.ComprobantePago;
import com.inklusport.subscriptions.entity.PagoEvento;
import com.inklusport.subscriptions.entity.PagoSuscripcion;
import com.inklusport.subscriptions.enums.TipoComprobante;
import com.inklusport.subscriptions.repository.ComprobantePagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComprobanteService {

    private final ComprobantePagoRepository comprobantePagoRepository;

    @Value("${app.comprobantes.storage-path:./comprobantes}")
    private String storagePath;

    @Transactional
    public ComprobantePago generarComprobanteEvento(PagoEvento pagoEvento, String concepto) {
        return generar(pagoEvento, null, TipoComprobante.INSCRIPCION, concepto, pagoEvento.getMonto(),
                pagoEvento.getMoneda(), pagoEvento.getReferenciaTransaccion(), pagoEvento.getFechaPago(),
                pagoEvento.getUsuarioId());
    }

    @Transactional
    public ComprobantePago generarComprobanteSuscripcion(PagoSuscripcion pagoSuscripcion, String concepto) {
        return generar(null, pagoSuscripcion, TipoComprobante.SUSCRIPCION, concepto, pagoSuscripcion.getMonto(),
                pagoSuscripcion.getMoneda(), pagoSuscripcion.getReferenciaTransaccion(), pagoSuscripcion.getFechaPago(),
                pagoSuscripcion.getSuscripcion().getOrganizadorId());
    }

    public File obtenerArchivo(ComprobantePago comprobante) {
        String ruta = comprobante.getUrlPdf();
        return (ruta != null && !ruta.isBlank()) ? new File(ruta) : null;
    }

    private ComprobantePago generar(PagoEvento pagoEvento, PagoSuscripcion pagoSuscripcion, TipoComprobante tipo,
                                    String concepto, BigDecimal monto, String moneda, String referencia,
                                    LocalDateTime fechaPago, String emailDestino) {
        String numero = "CMP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        File pdf = generarPdf(numero, concepto, monto, moneda, referencia, fechaPago);

        ComprobantePago comprobante = new ComprobantePago();
        comprobante.setPagoEvento(pagoEvento);
        comprobante.setPagoSuscripcion(pagoSuscripcion);
        comprobante.setTransaccion(pagoEvento != null ? pagoEvento.getTransaccion()
                : (pagoSuscripcion != null ? pagoSuscripcion.getTransaccion() : null));
        comprobante.setNumeroComprobante(numero);
        comprobante.setNumeroTransaccion(referencia);
        comprobante.setTipo(tipo);
        comprobante.setMonto(monto);
        comprobante.setMoneda(moneda != null ? moneda : "COP");
        comprobante.setDetalleEvento(concepto);
        comprobante.setEmailDestino(emailDestino);
        comprobante.setUrlPdf(pdf != null ? pdf.getPath() : null);
        return comprobantePagoRepository.save(comprobante);
    }

    private File generarPdf(String numero, String concepto, BigDecimal monto, String moneda, String referencia,
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
            document.add(new Paragraph("Monto pagado: " + (moneda != null ? moneda : "COP") + " " + monto.toPlainString(), normalFont));
            document.add(new Paragraph("Referencia de transaccion: " + (referencia != null ? referencia : "N/A"), normalFont));
            document.add(new Paragraph("Fecha de pago: " + fechaPago, normalFont));
            document.close();
            return file;
        } catch (Exception e) {
            log.error("Error generando el PDF del comprobante {}: {}", numero, e.getMessage(), e);
            return null;
        }
    }
}
