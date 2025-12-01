// =====================================================
// src/main/java/com/tecnano/factura/service/PdfFacturaService.java
// =====================================================
package com.tecnano.factura.service;

import com.tecnano.factura.config.AppProperties;
import com.tecnano.factura.model.DetalleVenta;
import com.tecnano.factura.model.Venta;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfFacturaService {

    private static final Logger log = LoggerFactory.getLogger(PdfFacturaService.class);

    // Fuentes compatibles con PDFBox 3 (sin usar constantes obsoletas)
    private static final PDFont FONT_BOLD =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_REGULAR =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_ITALIC =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    private final AppProperties appProperties;

    public PdfFacturaService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public byte[] generarRide(Venta venta) {
        if (venta == null) {
            log.warn("generarRide llamado con venta = null");
            return new byte[0];
        }

        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                dibujarFactura(venta, content, page);
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error generando PDF de factura para venta {}: {}", venta.getId(), e.getMessage(), e);
            return new byte[0];
        }
    }

    private void dibujarFactura(Venta venta, PDPageContentStream content, PDPage page) throws IOException {
        float margin = 40f;
        float y = page.getMediaBox().getHeight() - margin;

        AppProperties.Negocio negocio = appProperties.getNegocio();
        String nombreNegocio = negocio.getNombre();
        String ruc = negocio.getRuc();
        String regimen = negocio.getRegimen();
        String direccion = negocio.getDireccion();
        String telefono = negocio.getTelefono();

        // ========= ENCABEZADO NEGOCIO =========
        content.beginText();
        content.setFont(FONT_BOLD, 14);
        content.newLineAtOffset(margin, y);
        content.showText(nombreNegocio != null ? nombreNegocio.toUpperCase(Locale.ROOT) : "MI NEGOCIO");
        content.endText();
        y -= 16;

        content.beginText();
        content.setFont(FONT_REGULAR, 9);
        content.newLineAtOffset(margin, y);
        content.showText("RUC: " + (ruc != null ? ruc : "-"));
        content.endText();
        y -= 12;

        if (regimen != null && !regimen.isBlank()) {
            content.beginText();
            content.setFont(FONT_REGULAR, 8);
            content.newLineAtOffset(margin, y);
            content.showText(regimen);
            content.endText();
            y -= 10;
        }

        if (direccion != null && !direccion.isBlank()) {
            content.beginText();
            content.setFont(FONT_REGULAR, 8);
            content.newLineAtOffset(margin, y);
            content.showText(direccion);
            content.endText();
            y -= 10;
        }

        if (telefono != null && !telefono.isBlank()) {
            content.beginText();
            content.setFont(FONT_REGULAR, 8);
            content.newLineAtOffset(margin, y);
            content.showText("Teléfono: " + telefono);
            content.endText();
            y -= 12;
        }

        y -= 5;
        drawLine(content, margin, y, page.getMediaBox().getWidth() - margin, y);
        y -= 15;

        // ========= TÍTULO FACTURA =========
        content.beginText();
        content.setFont(FONT_BOLD, 11);
        content.newLineAtOffset(margin, y);
        content.showText("FACTURA / COMPROBANTE DE VENTA");
        content.endText();
        y -= 16;

        if (venta.getFechaHora() != null) {
            String fechaStr = venta.getFechaHora()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            content.beginText();
            content.setFont(FONT_REGULAR, 9);
            content.newLineAtOffset(margin, y);
            content.showText("Fecha: " + fechaStr);
            content.endText();
            y -= 12;
        }

        content.beginText();
        content.setFont(FONT_REGULAR, 9);
        content.newLineAtOffset(margin, y);
        content.showText("N° Venta: " + (venta.getId() != null ? venta.getId() : "-"));
        content.endText();
        y -= 12;

        if (venta.getClaveAccesoSri() != null) {
            content.beginText();
            content.setFont(FONT_REGULAR, 8);
            content.newLineAtOffset(margin, y);
            content.showText("Clave Acceso SRI: " + venta.getClaveAccesoSri());
            content.endText();
            y -= 10;
        }

        if (venta.getNumeroAutorizacionSri() != null) {
            content.beginText();
            content.setFont(FONT_REGULAR, 8);
            content.newLineAtOffset(margin, y);
            content.showText("Autorización SRI: " + venta.getNumeroAutorizacionSri());
            content.endText();
            y -= 10;
        }

        if (venta.getUbicacion() != null) {
            content.beginText();
            content.setFont(FONT_REGULAR, 9);
            content.newLineAtOffset(margin, y);
            content.showText("Mesa/Pedido: " + venta.getUbicacion());
            content.endText();
            y -= 12;
        }

        if (venta.getEstadoSri() != null) {
            content.beginText();
            content.setFont(FONT_REGULAR, 9);
            content.newLineAtOffset(margin, y);
            content.showText("Estado SRI: " + venta.getEstadoSri());
            content.endText();
            y -= 12;
        }

        // ========= CLIENTE =========
        if (venta.getCliente() != null) {
            y -= 5;
            drawLine(content, margin, y, page.getMediaBox().getWidth() - margin, y);
            y -= 14;

            var cliente = venta.getCliente();
            content.beginText();
            content.setFont(FONT_BOLD, 9);
            content.newLineAtOffset(margin, y);
            content.showText("Cliente:");
            content.endText();
            y -= 12;

            if (cliente.getNombre() != null) {
                content.beginText();
                content.setFont(FONT_REGULAR, 9);
                content.newLineAtOffset(margin, y);
                content.showText("Nombre/Razón social: " + cliente.getNombre());
                content.endText();
                y -= 12;
            }

            if (cliente.getNumeroIdentificacion() != null) {
                content.beginText();
                content.setFont(FONT_REGULAR, 9);
                content.newLineAtOffset(margin, y);
                content.showText("Documento: " + cliente.getNumeroIdentificacion());
                content.endText();
                y -= 12;
            }

            if (cliente.getCorreo() != null) {
                content.beginText();
                content.setFont(FONT_REGULAR, 9);
                content.newLineAtOffset(margin, y);
                content.showText("Correo: " + cliente.getCorreo());
                content.endText();
                y -= 12;
            }
        }

        y -= 5;
        drawLine(content, margin, y, page.getMediaBox().getWidth() - margin, y);
        y -= 14;

        // ========= CABECERA DETALLE =========
        float xCodigo = margin;
        float xDescripcion = margin + 60;
        float xCant = margin + 260;
        float xPrecio = margin + 310;
        float xSubtotal = margin + 370;

        content.beginText();
        content.setFont(FONT_BOLD, 8);
        content.newLineAtOffset(xCodigo, y);
        content.showText("COD");
        content.endText();

        content.beginText();
        content.setFont(FONT_BOLD, 8);
        content.newLineAtOffset(xDescripcion, y);
        content.showText("DESCRIPCIÓN");
        content.endText();

        content.beginText();
        content.setFont(FONT_BOLD, 8);
        content.newLineAtOffset(xCant, y);
        content.showText("CANT");
        content.endText();

        content.beginText();
        content.setFont(FONT_BOLD, 8);
        content.newLineAtOffset(xPrecio, y);
        content.showText("P.UNIT");
        content.endText();

        content.beginText();
        content.setFont(FONT_BOLD, 8);
        content.newLineAtOffset(xSubtotal, y);
        content.showText("SUBTOTAL");
        content.endText();

        y -= 10;
        drawLine(content, margin, y, page.getMediaBox().getWidth() - margin, y);
        y -= 10;

        // ========= DETALLE LÍNEA A LÍNEA =========
        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (DetalleVenta d : venta.getDetalles()) {
                if (d == null || d.getProducto() == null) continue;

                String codigo = d.getProducto().getCodigo() != null
                        ? d.getProducto().getCodigo()
                        : "";
                String nombreProd = d.getProducto().getNombre() != null
                        ? d.getProducto().getNombre()
                        : "(sin nombre)";
                int cantidad = d.getCantidad();
                BigDecimal precioUnitario = d.getPrecioUnitario() != null
                        ? d.getPrecioUnitario()
                        : BigDecimal.ZERO;
                BigDecimal subtotalLinea = precioUnitario.multiply(BigDecimal.valueOf(cantidad));

                content.beginText();
                content.setFont(FONT_REGULAR, 8);
                content.newLineAtOffset(xCodigo, y);
                content.showText(recortar(codigo, 8));
                content.endText();

                content.beginText();
                content.setFont(FONT_REGULAR, 8);
                content.newLineAtOffset(xDescripcion, y);
                content.showText(recortar(nombreProd, 30));
                content.endText();

                content.beginText();
                content.setFont(FONT_REGULAR, 8);
                content.newLineAtOffset(xCant, y);
                content.showText(String.valueOf(cantidad));
                content.endText();

                content.beginText();
                content.setFont(FONT_REGULAR, 8);
                content.newLineAtOffset(xPrecio, y);
                content.showText(precioUnitario
                        .setScale(2, RoundingMode.HALF_UP)
                        .toPlainString());
                content.endText();

                content.beginText();
                content.setFont(FONT_REGULAR, 8);
                content.newLineAtOffset(xSubtotal, y);
                content.showText(subtotalLinea
                        .setScale(2, RoundingMode.HALF_UP)
                        .toPlainString());
                content.endText();

                y -= 12;

                if (d.getNotaCocina() != null && !d.getNotaCocina().isBlank()) {
                    content.beginText();
                    content.setFont(FONT_ITALIC, 7);
                    content.newLineAtOffset(xDescripcion, y);
                    content.showText("Nota: " + recortar(d.getNotaCocina(), 45));
                    content.endText();
                    y -= 10;
                }

                if (y < 80) {
                    // Si se acaba la página, aquí podrías crear otra página.
                    break;
                }
            }
        } else {
            content.beginText();
            content.setFont(FONT_REGULAR, 8);
            content.newLineAtOffset(margin, y);
            content.showText("(Sin detalles de productos)");
            content.endText();
            y -= 12;
        }

        y -= 5;
        drawLine(content, margin, y, page.getMediaBox().getWidth() - margin, y);
        y -= 14;

        // ========= TOTALES =========
        BigDecimal subtotal = venta.getSubtotal();
        BigDecimal iva = venta.getIva();
        BigDecimal total = venta.getTotal();

        float xLabel = margin + 260;
        float xValue = margin + 370;

        content.beginText();
        content.setFont(FONT_REGULAR, 9);
        content.newLineAtOffset(xLabel, y);
        content.showText("Subtotal:");
        content.endText();

        content.beginText();
        content.setFont(FONT_REGULAR, 9);
        content.newLineAtOffset(xValue, y);
        content.showText(subtotal
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString());
        content.endText();
        y -= 12;

        content.beginText();
        content.setFont(FONT_REGULAR, 9);
        content.newLineAtOffset(xLabel, y);
        content.showText("IVA:");
        content.endText();

        content.beginText();
        content.setFont(FONT_REGULAR, 9);
        content.newLineAtOffset(xValue, y);
        content.showText(iva
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString());
        content.endText();
        y -= 12;

        content.beginText();
        content.setFont(FONT_BOLD, 10);
        content.newLineAtOffset(xLabel, y);
        content.showText("TOTAL:");
        content.endText();

        content.beginText();
        content.setFont(FONT_BOLD, 10);
        content.newLineAtOffset(xValue, y);
        content.showText(total
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString());
        content.endText();
        y -= 18;

        // ========= MENSAJE FINAL =========
        content.beginText();
        content.setFont(FONT_ITALIC, 8);
        content.newLineAtOffset(margin, y);
        content.showText("¡Gracias por su compra!");
        content.endText();
    }

    private void drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2) throws IOException {
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private String recortar(String texto, int max) {
        if (texto == null) return "";
        if (texto.length() <= max) return texto;
        return texto.substring(0, max - 3) + "...";
    }
}
