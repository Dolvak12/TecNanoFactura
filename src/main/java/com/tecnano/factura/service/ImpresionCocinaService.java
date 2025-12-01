// =====================================================
// src/main/java/com/tecnano/factura/service/ImpresionCocinaService.java
// =====================================================
package com.tecnano.factura.service;

import com.tecnano.factura.config.AppProperties;
import com.tecnano.factura.model.DetalleVenta;
import com.tecnano.factura.model.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ImpresionCocinaService {

    private static final Logger log = LoggerFactory.getLogger(ImpresionCocinaService.class);

    private final AppProperties appProperties;

    public ImpresionCocinaService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void imprimirComanda(Venta venta) {
        if (venta == null) {
            log.warn("imprimirComanda llamada con venta = null; se ignora.");
            return;
        }

        String impresoraNombre = appProperties.getImpresion().getImpresoraCocina();
        if (impresoraNombre == null || impresoraNombre.isBlank()) {
            log.info("No hay impresora de cocina configurada. Se omite impresión de comanda para venta {}.",
                    venta.getId());
            return;
        }

        PrintService impresora = buscarImpresoraPorNombre(impresoraNombre);
        if (impresora == null) {
            log.warn("No se encontró impresora de cocina con nombre que contenga '{}'.", impresoraNombre);
            return;
        }

        String contenido = generarTextoComanda(venta);

        try {
            enviarAImpresora(impresora, contenido);
            log.info("Comanda de cocina enviada a la impresora '{}' para venta {}",
                    impresora.getName(), venta.getId());
        } catch (Exception e) {
            log.error("Error al imprimir comanda en impresora '{}': {}",
                    impresora.getName(), e.getMessage(), e);
        }
    }

    private PrintService buscarImpresoraPorNombre(String nombreParcial) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services == null || services.length == 0) {
            log.warn("No se encontraron impresoras instaladas en el sistema.");
            return null;
        }

        String buscado = nombreParcial.toLowerCase(Locale.ROOT);

        for (PrintService ps : services) {
            String actual = ps.getName() != null ? ps.getName().toLowerCase(Locale.ROOT) : "";
            if (actual.contains(buscado)) {
                return ps;
            }
        }

        log.warn("No se encontró impresora cuyo nombre contenga '{}'.", nombreParcial);
        return null;
    }

    private void enviarAImpresora(PrintService impresora, String contenido) throws Exception {
        byte[] bytes = contenido.getBytes(StandardCharsets.UTF_8);

        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        DocPrintJob job = impresora.createPrintJob();
        Doc doc = new SimpleDoc(bytes, flavor, null);
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();

        job.print(doc, attrs);
    }

    private String generarTextoComanda(Venta venta) {
        StringBuilder sb = new StringBuilder();

        String nombreNegocio = appProperties.getNegocio().getNombre();
        String ubicacion = venta.getUbicacion();
        String tipoCliente = venta.getTipoCliente();
        String formaPago = venta.getFormaPago();

        sb.append("   *** COMANDA COCINA ***   ").append("\n");
        if (nombreNegocio != null && !nombreNegocio.isBlank()) {
            sb.append(nombreNegocio.toUpperCase(Locale.ROOT)).append("\n");
        }
        sb.append("------------------------------").append("\n");

        if (venta.getFechaHora() != null) {
            String fechaStr = venta.getFechaHora()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            sb.append("Fecha: ").append(fechaStr).append("\n");
        }

        sb.append("Venta N°: ").append(venta.getId() != null ? venta.getId() : "-").append("\n");
        sb.append("Mesa/pedido: ").append(ubicacion != null ? ubicacion : "-").append("\n");

        if (tipoCliente != null) {
            sb.append("Cliente: ")
                    .append("CON_DATOS".equalsIgnoreCase(tipoCliente) ? "Con datos" : "Consumidor final")
                    .append("\n");
        }

        if (formaPago != null) {
            sb.append("Pago: ").append(formaPago).append("\n");
        }

        sb.append("------------------------------").append("\n");
        sb.append("CANT  PRODUCTO").append("\n");
        sb.append("------------------------------").append("\n");

        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (DetalleVenta d : venta.getDetalles()) {
                if (d == null || d.getProducto() == null) continue;

                int cant = d.getCantidad();
                String nombreProd = d.getProducto().getNombre() != null
                        ? d.getProducto().getNombre()
                        : "(sin nombre)";

                sb.append(String.format(Locale.ROOT, "%-4s %s", cant, nombreProd))
                        .append("\n");

                String nota = d.getNotaCocina();
                if (nota != null && !nota.isBlank()) {
                    sb.append("   -> ").append(nota).append("\n");
                }
            }
        } else {
            sb.append("(Sin detalles de productos)").append("\n");
        }

        sb.append("------------------------------").append("\n");
        sb.append("   FIN COMANDA COCINA   ").append("\n\n\n");
        sb.append("\n\n\n");

        return sb.toString();
    }
}
