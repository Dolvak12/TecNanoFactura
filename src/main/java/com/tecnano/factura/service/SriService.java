// =====================================================
// src/main/java/com/tecnano/factura/service/SriService.java
// =====================================================
package com.tecnano.factura.service;

import com.tecnano.factura.config.AppProperties;
import com.tecnano.factura.model.Cliente;
import com.tecnano.factura.model.DetalleVenta;
import com.tecnano.factura.model.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
public class SriService {

    private static final Logger log = LoggerFactory.getLogger(SriService.class);

    private final AppProperties appProperties;
    private final PdfFacturaService pdfFacturaService;
    private final ProveedorSriApiClient proveedorSriApiClient;

    public SriService(AppProperties appProperties,
                      PdfFacturaService pdfFacturaService,
                      ProveedorSriApiClient proveedorSriApiClient) {
        this.appProperties = appProperties;
        this.pdfFacturaService = pdfFacturaService;
        this.proveedorSriApiClient = proveedorSriApiClient;
    }

    public void procesarVentaConSri(Venta venta) {
        if (venta == null) {
            log.warn("procesarVentaConSri llamado con venta = null");
            return;
        }

        try {
            byte[] xmlBytes = generarXmlBasico(venta).getBytes(StandardCharsets.UTF_8);
            byte[] pdfRideLocal = pdfFacturaService.generarRide(venta);

            ProveedorSriApiClient.ResultadoEnvio resultado =
                    proveedorSriApiClient.enviarFactura(venta, xmlBytes, pdfRideLocal);

            venta.setEstadoSri(resultado.getEstadoSri());
            venta.setMensajeErrorSri(resultado.getMensajeError());
            venta.setClaveAccesoSri(resultado.getClaveAccesoSri());
            venta.setNumeroAutorizacionSri(resultado.getNumeroAutorizacionSri());

            byte[] rideFinal = resultado.getRidePdf() != null
                    ? resultado.getRidePdf()
                    : pdfRideLocal;

            if (rideFinal != null && rideFinal.length > 0) {
                venta.setRidePdf(rideFinal);
            }

            log.info("Venta {} procesada con SRI. Estado={}", venta.getId(), venta.getEstadoSri());

        } catch (Exception ex) {
            log.error("Error procesando venta {} con SRI: {}", venta.getId(), ex.getMessage(), ex);
            venta.setEstadoSri("ERROR");
            venta.setMensajeErrorSri("Error al procesar con SRI: " + ex.getMessage());
        }
    }

    private String generarXmlBasico(Venta venta) {
        AppProperties.Negocio negocio = appProperties.getNegocio();
        Cliente cli = venta.getCliente();

        String nombreNegocio = safe(negocio.getNombre());
        String ruc = safe(negocio.getRuc());
        String regimen = safe(negocio.getRegimen());
        String direccion = safe(negocio.getDireccion());
        String telefono = safe(negocio.getTelefono());

        String fechaEmision = "";
        if (venta.getFechaHora() != null) {
            fechaEmision = venta.getFechaHora()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        }

        String tipoCliente = venta.getTipoCliente() != null ? venta.getTipoCliente() : "FINAL";
        String formaPago = venta.getFormaPago() != null ? venta.getFormaPago() : "Efectivo";

        BigDecimal subtotal = venta.getSubtotal();
        BigDecimal iva = venta.getIva();
        BigDecimal total = venta.getTotal();

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<factura>\n");

        sb.append("  <emisor>\n");
        sb.append("    <nombreComercial>").append(escapeXml(nombreNegocio)).append("</nombreComercial>\n");
        sb.append("    <ruc>").append(escapeXml(ruc)).append("</ruc>\n");
        sb.append("    <regimen>").append(escapeXml(regimen)).append("</regimen>\n");
        sb.append("    <direccion>").append(escapeXml(direccion)).append("</direccion>\n");
        sb.append("    <telefono>").append(escapeXml(telefono)).append("</telefono>\n");
        sb.append("  </emisor>\n");

        sb.append("  <comprobante>\n");
        sb.append("    <numeroInterno>").append(venta.getId() != null ? venta.getId() : 0L).append("</numeroInterno>\n");
        sb.append("    <fechaEmision>").append(escapeXml(fechaEmision)).append("</fechaEmision>\n");
        sb.append("    <mesaUbicacion>").append(escapeXml(safe(venta.getUbicacion()))).append("</mesaUbicacion>\n");
        sb.append("    <tipoCliente>").append(escapeXml(tipoCliente)).append("</tipoCliente>\n");
        sb.append("    <formaPago>").append(escapeXml(formaPago)).append("</formaPago>\n");
        sb.append("  </comprobante>\n");

        if (cli != null) {
            sb.append("  <cliente>\n");
            sb.append("    <nombre>").append(escapeXml(safe(cli.getNombre()))).append("</nombre>\n");
            sb.append("    <numeroIdentificacion>")
                    .append(escapeXml(safe(cli.getNumeroIdentificacion())))
                    .append("</numeroIdentificacion>\n");
            sb.append("    <correo>").append(escapeXml(safe(cli.getCorreo()))).append("</correo>\n");
            sb.append("  </cliente>\n");
        }

        sb.append("  <detalles>\n");
        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (DetalleVenta d : venta.getDetalles()) {
                if (d == null || d.getProducto() == null) continue;

                String codigo = safe(d.getProducto().getCodigo());
                String nombreProd = safe(d.getProducto().getNombre());
                int cantidad = d.getCantidad();
                BigDecimal precioUnitario = d.getPrecioUnitario() != null
                        ? d.getPrecioUnitario()
                        : BigDecimal.ZERO;
                BigDecimal subtotalLinea = precioUnitario.multiply(BigDecimal.valueOf(cantidad));

                sb.append("    <detalle>\n");
                sb.append("      <codigo>").append(escapeXml(codigo)).append("</codigo>\n");
                sb.append("      <descripcion>").append(escapeXml(nombreProd)).append("</descripcion>\n");
                sb.append("      <cantidad>").append(cantidad).append("</cantidad>\n");
                sb.append("      <precioUnitario>")
                        .append(precioUnitario.setScale(2, BigDecimal.ROUND_HALF_UP))
                        .append("</precioUnitario>\n");
                sb.append("      <subtotal>")
                        .append(subtotalLinea.setScale(2, BigDecimal.ROUND_HALF_UP))
                        .append("</subtotal>\n");
                if (d.getNotaCocina() != null && !d.getNotaCocina().isBlank()) {
                    sb.append("      <nota>")
                            .append(escapeXml(safe(d.getNotaCocina())))
                            .append("</nota>\n");
                }
                sb.append("    </detalle>\n");
            }
        }
        sb.append("  </detalles>\n");

        sb.append("  <totales>\n");
        sb.append("    <subtotal>")
                .append(subtotal.setScale(2, BigDecimal.ROUND_HALF_UP))
                .append("</subtotal>\n");
        sb.append("    <iva>")
                .append(iva.setScale(2, BigDecimal.ROUND_HALF_UP))
                .append("</iva>\n");
        sb.append("    <total>")
                .append(total.setScale(2, BigDecimal.ROUND_HALF_UP))
                .append("</total>\n");
        sb.append("  </totales>\n");

        sb.append("</factura>\n");

        return sb.toString();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
