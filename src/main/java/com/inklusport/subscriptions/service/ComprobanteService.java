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
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Servicio de generación y almacenamiento de comprobantes de pago.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprobanteService {

    private static final Color ROJO_MARCA = new Color(0xA3, 0x0D, 0x11);
    private static final Color TEXTO_OSCURO = new Color(0x0F, 0x17, 0x2A);
    private static final Color GRIS_SUAVE = new Color(0x64, 0x74, 0x8B);
    private static final Color GRIS_FONDO = new Color(0xF8, 0xFA, 0xFC);
    private static final Color GRIS_BORDE = new Color(0xE2, 0xE8, 0xF0);
    private static final Color VERDE_TEXTO = new Color(0x15, 0x80, 0x3D);
    private static final Color VERDE_FONDO = new Color(0xDC, 0xFC, 0xE7);

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LOGO_CLASSPATH = "branding/inklusport-logo.png";

    private final ComprobantePagoRepository comprobantePagoRepository;

    @Value("${app.comprobantes.storage-path:./comprobantes}")
    private String storagePath;

    /**
     * Genera el comprobante de un pago de inscripción a evento.
     *
     * @param pagoEvento pago de evento asociado
     * @param concepto   descripción del cobro
     * @return comprobante persistido
     */
    public ComprobantePago generarComprobanteEvento(PagoEvento pagoEvento, String concepto) {
        return generar(pagoEvento, null, TipoComprobante.INSCRIPCION, concepto, pagoEvento.getMonto(),
                pagoEvento.getMoneda(), pagoEvento.getReferenciaTransaccion(), pagoEvento.getFechaPago(),
                pagoEvento.getUsuarioId());
    }

    /**
     * Genera el comprobante de un pago de suscripción.
     *
     * @param pagoSuscripcion pago de suscripción asociado
     * @param concepto        descripción del cobro
     * @return comprobante persistido
     */
    public ComprobantePago generarComprobanteSuscripcion(PagoSuscripcion pagoSuscripcion, String concepto) {
        return generar(null, pagoSuscripcion, TipoComprobante.SUSCRIPCION, concepto, pagoSuscripcion.getMonto(),
                pagoSuscripcion.getMoneda(), pagoSuscripcion.getReferenciaTransaccion(), pagoSuscripcion.getFechaPago(),
                pagoSuscripcion.getOrganizadorId());
    }

    /**
     * Obtiene el archivo PDF del comprobante, si existe.
     *
     * @param comprobante comprobante con la ruta del PDF
     * @return archivo o {@code null} si no hay ruta
     */
    public File obtenerArchivo(ComprobantePago comprobante) {
        String ruta = comprobante.getUrlPdf();
        return (ruta != null && !ruta.isBlank()) ? new File(ruta) : null;
    }

    /**
     * Si el PDF se perdió del disco (p. ej. contenedor recreado sin volumen), lo vuelve a generar
     * con los datos del comprobante y actualiza {@code url_pdf}.
     */
    public File asegurarArchivo(ComprobantePago comprobante) {
        File archivo = obtenerArchivo(comprobante);
        if (archivo != null && archivo.exists()) {
            return archivo;
        }
        File regenerado = generarPdf(
                comprobante.getNumeroComprobante(),
                comprobante.getTipo(),
                comprobante.getDetalleEvento(),
                comprobante.getMonto(),
                comprobante.getMoneda(),
                comprobante.getNumeroTransaccion(),
                comprobante.getFechaGeneracion() != null ? comprobante.getFechaGeneracion() : LocalDateTime.now());
        if (regenerado == null) {
            return null;
        }
        comprobante.setUrlPdf(regenerado.getPath());
        comprobantePagoRepository.save(comprobante);
        log.warn("PDF del comprobante {} regenerado en {}", comprobante.getNumeroComprobante(), regenerado.getPath());
        return regenerado;
    }

    /**
     * Crea el PDF, arma la entidad y la guarda.
     *
     * @param pagoEvento      pago de evento, o {@code null}
     * @param pagoSuscripcion pago de suscripción, o {@code null}
     * @param tipo            tipo de comprobante
     * @param concepto        descripción del cobro
     * @param monto           monto pagado
     * @param moneda          moneda del pago
     * @param referencia      referencia de la transacción
     * @param fechaPago       fecha del pago
     * @param emailDestino    destinatario del comprobante
     * @return comprobante persistido
     */
    private ComprobantePago generar(PagoEvento pagoEvento, PagoSuscripcion pagoSuscripcion, TipoComprobante tipo,
                                    String concepto, BigDecimal monto, String moneda, String referencia,
                                    LocalDateTime fechaPago, String emailDestino) {
        String numero = "CMP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        File pdf = generarPdf(numero, tipo, concepto, monto, moneda, referencia, fechaPago);

        ComprobantePago comprobante = new ComprobantePago();
        comprobante.setPagoEventoId(pagoEvento != null ? pagoEvento.getId() : null);
        comprobante.setPagoSuscripcionId(pagoSuscripcion != null ? pagoSuscripcion.getId() : null);
        comprobante.setTransaccionId(pagoEvento != null ? pagoEvento.getTransaccionId()
                : (pagoSuscripcion != null ? pagoSuscripcion.getTransaccionId() : null));
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

    /**
     * Genera el PDF del comprobante en el directorio de almacenamiento, con
     * el membrete y los colores de marca de InkluSport.
     *
     * @param numero     número de comprobante
     * @param tipo       tipo de comprobante
     * @param concepto   descripción del cobro
     * @param monto      monto pagado
     * @param moneda     moneda del pago
     * @param referencia referencia de la transacción
     * @param fechaPago  fecha del pago
     * @return archivo PDF o {@code null} si falla la generación
     */
    private File generarPdf(String numero, TipoComprobante tipo, String concepto, BigDecimal monto, String moneda,
                            String referencia, LocalDateTime fechaPago) {
        try {
            File dir = new File(storagePath);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new java.io.IOException("No se pudo crear el directorio de comprobantes: " + storagePath);
            }
            File file = new File(dir, numero + ".pdf");

            Document document = new Document(PageSize.A4, 42, 42, 36, 42);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.add(construirEncabezado());
            document.add(construirTitulo(numero, tipo));
            document.add(construirBloqueDetalle(tipo, concepto, referencia, fechaPago));
            document.add(construirMontoDestacado(monto, moneda));
            document.add(construirPiePagina());

            document.close();
            return file;
        } catch (Exception e) {
            log.error("Error generando el PDF del comprobante {}: {}", numero, e.getMessage(), e);
            return null;
        }
    }

    /** Franja roja superior con el logo y el nombre de la plataforma. */
    private PdfPTable construirEncabezado() {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        try {
            tabla.setWidths(new float[]{1f, 4f});
        } catch (Exception ignored) {
            // ancho por defecto si las columnas no se pueden ajustar
        }

        PdfPCell celdaLogo = new PdfPCell();
        celdaLogo.setBackgroundColor(ROJO_MARCA);
        celdaLogo.setBorder(Rectangle.NO_BORDER);
        celdaLogo.setPadding(12);
        celdaLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Image logo = cargarLogo();
        if (logo != null) {
            celdaLogo.addElement(logo);
        }

        PdfPCell celdaMarca = new PdfPCell();
        celdaMarca.setBackgroundColor(ROJO_MARCA);
        celdaMarca.setBorder(Rectangle.NO_BORDER);
        celdaMarca.setPadding(14);
        celdaMarca.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Font fuenteMarca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
        Font fuenteEslogan = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(0xFF, 0xE1, 0xE2));
        Paragraph marca = new Paragraph("INKLUSPORT", fuenteMarca);
        marca.add(new Chunk("\nPlataforma de gestión deportiva adaptada", fuenteEslogan));
        celdaMarca.addElement(marca);

        tabla.addCell(celdaLogo);
        tabla.addCell(celdaMarca);
        return tabla;
    }

    /** Carga el isotipo de InkluSport desde los recursos del servicio. */
    private Image cargarLogo() {
        try {
            ClassPathResource recurso = new ClassPathResource(LOGO_CLASSPATH);
            Image logo = Image.getInstance(recurso.getInputStream().readAllBytes());
            logo.scaleToFit(38, 38);
            return logo;
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo para el comprobante: {}", e.getMessage());
            return null;
        }
    }

    /** Título del documento, número de comprobante y distintivo de estado pagado. */
    private PdfPTable construirTitulo(String numero, TipoComprobante tipo) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(22);
        tabla.setSpacingAfter(16);
        try {
            tabla.setWidths(new float[]{2.4f, 1f});
        } catch (Exception ignored) {
            // ancho por defecto si las columnas no se pueden ajustar
        }

        Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, TEXTO_OSCURO);
        Font fuenteSub = FontFactory.getFont(FontFactory.HELVETICA, 10, GRIS_SUAVE);

        PdfPCell celdaTitulo = new PdfPCell();
        celdaTitulo.setBorder(Rectangle.NO_BORDER);
        celdaTitulo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaTitulo.addElement(new Paragraph("Comprobante de pago", fuenteTitulo));
        celdaTitulo.addElement(new Paragraph("N.° " + numero, fuenteSub));

        Font fuenteBadge = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, VERDE_TEXTO);
        PdfPCell celdaBadge = new PdfPCell(new Phrase("PAGADO", fuenteBadge));
        celdaBadge.setBackgroundColor(VERDE_FONDO);
        celdaBadge.setBorder(Rectangle.NO_BORDER);
        celdaBadge.setPadding(10);
        celdaBadge.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaBadge.setVerticalAlignment(Element.ALIGN_MIDDLE);

        tabla.addCell(celdaTitulo);
        tabla.addCell(celdaBadge);
        return tabla;
    }

    /** Tabla de detalle: tipo de pago, concepto, fecha y referencia. */
    private PdfPTable construirBloqueDetalle(TipoComprobante tipo, String concepto, String referencia,
                                             LocalDateTime fechaPago) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(14);
        try {
            tabla.setWidths(new float[]{1f, 1.5f});
        } catch (Exception ignored) {
            // ancho por defecto si las columnas no se pueden ajustar
        }

        agregarFila(tabla, "Tipo de pago", etiquetaTipo(tipo), false);
        agregarFila(tabla, "Concepto", concepto != null && !concepto.isBlank() ? concepto : "—", true);
        agregarFila(tabla, "Fecha de pago",
                fechaPago != null ? FORMATO_FECHA.format(fechaPago) : "—", false);
        agregarFila(tabla, "Referencia de transacción",
                referencia != null && !referencia.isBlank() ? referencia : "N/A", true);

        return tabla;
    }

    private void agregarFila(PdfPTable tabla, String etiqueta, String valor, boolean fondoAlterno) {
        Font fuenteEtiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, GRIS_SUAVE);
        Font fuenteValor = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXTO_OSCURO);
        Color fondo = fondoAlterno ? GRIS_FONDO : Color.WHITE;

        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta.toUpperCase(Locale.ROOT), fuenteEtiqueta));
        celdaEtiqueta.setBorder(Rectangle.BOTTOM);
        celdaEtiqueta.setBorderColor(GRIS_BORDE);
        celdaEtiqueta.setBackgroundColor(fondo);
        celdaEtiqueta.setPadding(10);
        celdaEtiqueta.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, fuenteValor));
        celdaValor.setBorder(Rectangle.BOTTOM);
        celdaValor.setBorderColor(GRIS_BORDE);
        celdaValor.setBackgroundColor(fondo);
        celdaValor.setPadding(10);
        celdaValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaValor.setVerticalAlignment(Element.ALIGN_MIDDLE);

        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }

    private String etiquetaTipo(TipoComprobante tipo) {
        return tipo == TipoComprobante.SUSCRIPCION ? "Pago de suscripción" : "Inscripción a evento";
    }

    /** Franja destacada con el monto total pagado. */
    private PdfPTable construirMontoDestacado(BigDecimal monto, String moneda) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(28);
        try {
            tabla.setWidths(new float[]{1f, 1f});
        } catch (Exception ignored) {
            // ancho por defecto si las columnas no se pueden ajustar
        }

        Font fuenteEtiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font fuenteMonto = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.WHITE);

        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase("TOTAL PAGADO", fuenteEtiqueta));
        celdaEtiqueta.setBackgroundColor(ROJO_MARCA);
        celdaEtiqueta.setBorder(Rectangle.NO_BORDER);
        celdaEtiqueta.setPadding(16);
        celdaEtiqueta.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell celdaMonto = new PdfPCell(new Phrase(formatearMonto(monto, moneda), fuenteMonto));
        celdaMonto.setBackgroundColor(ROJO_MARCA);
        celdaMonto.setBorder(Rectangle.NO_BORDER);
        celdaMonto.setPadding(16);
        celdaMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaMonto.setVerticalAlignment(Element.ALIGN_MIDDLE);

        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaMonto);
        return tabla;
    }

    private String formatearMonto(BigDecimal monto, String moneda) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        String valor = monto != null ? formato.format(monto) : "0.00";
        String simbolo = moneda != null && !moneda.isBlank() ? moneda : "COP";
        return "$ " + valor + " " + simbolo;
    }

    /** Pie de página con los datos legales del comprobante. */
    private Paragraph construirPiePagina() {
        Font fuentePie = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS_SUAVE);
        Paragraph pie = new Paragraph();
        pie.setAlignment(Element.ALIGN_CENTER);
        pie.add(new Chunk("InkluSport S.A.S. · Plataforma de gestión deportiva adaptada", fuentePie));
        pie.add(new Chunk("\nEste comprobante fue generado automáticamente y no requiere firma.", fuentePie));
        return pie;
    }
}
