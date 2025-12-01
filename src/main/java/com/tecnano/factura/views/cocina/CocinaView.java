package com.tecnano.factura.views.cocina;

import com.tecnano.factura.model.DetalleVenta;
import com.tecnano.factura.model.Venta;
import com.tecnano.factura.repository.VentaRepository;
import com.tecnano.factura.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Route(value = "cocina", layout = MainLayout.class)
@PageTitle("Cocina")
@PermitAll
public class CocinaView extends VerticalLayout {

    private final VentaRepository ventaRepository;

    // Contenedores por estado
    private final VerticalLayout nuevosLayout = new VerticalLayout();
    private final VerticalLayout enPrepLayout = new VerticalLayout();
    private final VerticalLayout listosLayout = new VerticalLayout();

    private final Span nuevosCount = new Span("0");
    private final Span enPrepCount = new Span("0");
    private final Span listosCount = new Span("0");

    private final DateTimeFormatter horaFormatter =
            DateTimeFormatter.ofPattern("HH:mm");

    public CocinaView(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        construirUI();
        cargarOrdenesHoy();
    }

    // =========================
    // Construcción UI
    // =========================
    private void construirUI() {
        H1 titulo = new H1("Pantalla de cocina");
        titulo.getStyle().set("margin-bottom", "0.25rem");

        Span ayuda = new Span("Aquí se muestran las órdenes del día, para que la cocina marque el estado de cada una.");
        ayuda.getStyle()
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(titulo, ayuda);
        header.setPadding(false);
        header.setSpacing(false);

        VerticalLayout columnaNuevos = crearColumnaEstado(
                "Nuevos",
                "Órdenes que acaban de entrar y aún no se han tomado.",
                nuevosLayout,
                nuevosCount
        );

        VerticalLayout columnaEnPrep = crearColumnaEstado(
                "En preparación",
                "Órdenes que ya están siendo preparadas.",
                enPrepLayout,
                enPrepCount
        );

        VerticalLayout columnaListos = crearColumnaEstado(
                "Listos para entregar",
                "Órdenes listas que esperan ser llevadas a mesa / mostrador.",
                listosLayout,
                listosCount
        );

        HorizontalLayout columnas = new HorizontalLayout(columnaNuevos, columnaEnPrep, columnaListos);
        columnas.setWidthFull();
        columnas.setSpacing(true);
        columnas.setAlignItems(FlexComponent.Alignment.START);
        columnas.setFlexGrow(1, columnaNuevos, columnaEnPrep, columnaListos);

        add(header, columnas);
    }

    private VerticalLayout crearColumnaEstado(String titulo,
                                              String descripcion,
                                              VerticalLayout contenedor,
                                              Span contador) {

        VerticalLayout card = new VerticalLayout();
        card.addClassName("card-blanca");
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);

        H3 title = new H3(titulo);
        title.getStyle().set("margin-top", "0").set("margin-bottom", "0.15rem");

        contador.getStyle()
                .set("font-size", "0.85rem")
                .set("color", "var(--lumo-secondary-text-color)");

        Span desc = new Span(descripcion);
        desc.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout header = new HorizontalLayout(title, contador);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);

        contenedor.setPadding(false);
        contenedor.setSpacing(true);
        contenedor.getStyle().set("margin-top", "0.5rem");

        card.add(header, desc, contenedor);

        return card;
    }

    // =========================
    // Carga de órdenes
    // =========================
    private void cargarOrdenesHoy() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime desde = hoy.atStartOfDay();
        LocalDateTime hasta = hoy.plusDays(1).atStartOfDay().minusNanos(1);

        List<Venta> ventasDia = ventaRepository.findByFechaHoraBetween(desde, hasta);

        // Limpiar columnas
        nuevosLayout.removeAll();
        enPrepLayout.removeAll();
        listosLayout.removeAll();

        int countNuevos = 0;
        int countEnPrep = 0;
        int countListos = 0;

        ventasDia.stream()
                .filter(v -> v.getTotal() != null) // solo ventas reales
                .sorted(Comparator.comparing(Venta::getFechaHora, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(venta -> {
                    String estado = normalizarEstado(venta.getEstadoCocina());
                    // No mostramos los ENTREGADA en la pantalla de cocina
                    if ("ENTREGADA".equals(estado)) {
                        return;
                    }

                    VerticalLayout cardOrden = crearCardOrden(venta, estado);

                    switch (estado) {
                        case "EN_PREPARACION" -> {
                            enPrepLayout.add(cardOrden);
                            enPrepLayout.setSpacing(true);
                        }
                        case "LISTA" -> {
                            listosLayout.add(cardOrden);
                            listosLayout.setSpacing(true);
                        }
                        default -> { // NULO o "NUEVO"
                            nuevosLayout.add(cardOrden);
                            nuevosLayout.setSpacing(true);
                        }
                    }
                });

        // Recontar
        countNuevos = nuevosLayout.getComponentCount();
        countEnPrep = enPrepLayout.getComponentCount();
        countListos = listosLayout.getComponentCount();

        nuevosCount.setText(countNuevos + " orden(es)");
        enPrepCount.setText(countEnPrep + " orden(es)");
        listosCount.setText(countListos + " orden(es)");
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) return "NUEVO";
        return estado.toUpperCase();
    }

    // =========================
    // Card de cada orden
    // =========================
    private VerticalLayout crearCardOrden(Venta venta, String estado) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("orden-cocina-card"); // puedes estilizar en pos.css
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "var(--lumo-base-color)")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.06)");

        // Encabezado: mesa + hora + total
        String ubic = venta.getUbicacion() != null ? venta.getUbicacion() : venta.getMesa();
        String hora = venta.getFechaHora() != null
                ? venta.getFechaHora().format(horaFormatter)
                : "--:--";

        Span linea1 = new Span("Mesa/pedido: " + (ubic != null ? ubic : "-"));
        linea1.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.9rem");

        Span linea2 = new Span("Hora: " + hora);
        linea2.getStyle().set("font-size", "0.8rem");

        BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
        Span totalSpan = new Span("Total: $" + total.setScale(2, RoundingMode.HALF_UP));
        totalSpan.getStyle()
                .set("font-size", "0.85rem")
                .set("font-weight", "600");

        HorizontalLayout header = new HorizontalLayout(linea1, totalSpan);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        // Detalle de platos en mini lista
        VerticalLayout detalleLayout = new VerticalLayout();
        detalleLayout.setPadding(false);
        detalleLayout.setSpacing(false);
        detalleLayout.getStyle().set("margin-top", "0.35rem");

        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            int maxLineas = 5; // para no desbordar mucho la tarjeta
            int idx = 0;
            for (DetalleVenta d : venta.getDetalles()) {
                if (d == null || d.getProducto() == null) continue;
                idx++;
                if (idx > maxLineas) {
                    Span mas = new Span("... (" + (venta.getDetalles().size() - maxLineas) + " ítem(s) más)");
                    mas.getStyle()
                            .set("font-size", "0.75rem")
                            .set("color", "var(--lumo-secondary-text-color)");
                    detalleLayout.add(mas);
                    break;
                }

                String nombre = d.getProducto().getNombre() != null
                        ? d.getProducto().getNombre()
                        : "(sin nombre)";
                int cantidad = d.getCantidad();
                String lineaProd = cantidad + " x " + nombre;

                Span sProd = new Span(lineaProd);
                sProd.getStyle().set("font-size", "0.8rem");
                detalleLayout.add(sProd);

                if (d.getNotaCocina() != null && !d.getNotaCocina().isBlank()) {
                    Span sNota = new Span("Nota: " + d.getNotaCocina());
                    sNota.getStyle()
                            .set("font-size", "0.75rem")
                            .set("font-style", "italic")
                            .set("color", "var(--lumo-secondary-text-color)");
                    detalleLayout.add(sNota);
                }
            }
        } else {
            Span sin = new Span("(Sin detalle de productos)");
            sin.getStyle()
                    .set("font-size", "0.8rem")
                    .set("color", "var(--lumo-secondary-text-color)");
            detalleLayout.add(sin);
        }

        // Botones de acción según estado
        HorizontalLayout acciones = new HorizontalLayout();
        acciones.setWidthFull();
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        acciones.setSpacing(true);
        acciones.getStyle().set("margin-top", "0.5rem");

        Button verDetalle = new Button("Ver detalle", e -> mostrarDialogoDetalle(venta));
        verDetalle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        if ("EN_PREPARACION".equals(estado)) {
            Button listoBtn = new Button("Marcar LISTO", e -> cambiarEstado(venta, "LISTA"));
            listoBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
            acciones.add(verDetalle, listoBtn);
        } else if ("LISTA".equals(estado)) {
            Button entregarBtn = new Button("Marcar ENTREGADA", e -> cambiarEstado(venta, "ENTREGADA"));
            entregarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            acciones.add(verDetalle, entregarBtn);
        } else {
            // NUEVO (o null)
            Button tomarBtn = new Button("Tomar orden", e -> cambiarEstado(venta, "EN_PREPARACION"));
            tomarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            acciones.add(verDetalle, tomarBtn);
        }

        card.add(header, linea2, detalleLayout, acciones);
        return card;
    }

    private void mostrarDialogoDetalle(Venta venta) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Detalle de orden #" + venta.getId());

        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(false);
        contenido.setSpacing(false);
        contenido.setWidthFull();

        String ubic = venta.getUbicacion() != null ? venta.getUbicacion() : venta.getMesa();
        String hora = venta.getFechaHora() != null
                ? venta.getFechaHora().format(horaFormatter)
                : "--:--";

        Span lineaMesa = new Span("Mesa/pedido: " + (ubic != null ? ubic : "-"));
        Span lineaHora = new Span("Hora: " + hora);
        BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
        Span lineaTotal = new Span("Total: $" + total.setScale(2, RoundingMode.HALF_UP));

        lineaMesa.getStyle().set("font-weight", "600");
        lineaTotal.getStyle().set("font-weight", "600");

        contenido.add(lineaMesa, lineaHora, lineaTotal);

        contenido.add(new Span("-----------------------------"));

        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (DetalleVenta d : venta.getDetalles()) {
                if (d == null || d.getProducto() == null) continue;

                String nombre = Objects.toString(d.getProducto().getNombre(), "(sin nombre)");
                int cantidad = d.getCantidad();
                BigDecimal precioUnit = d.getPrecioUnitario() != null
                        ? d.getPrecioUnitario()
                        : BigDecimal.ZERO;
                BigDecimal subtotal = precioUnit.multiply(BigDecimal.valueOf(cantidad));

                Span linea = new Span(
                        cantidad + " x " + nombre +
                                "  -  $" + precioUnit.setScale(2, RoundingMode.HALF_UP) +
                                "  (Sub: $" + subtotal.setScale(2, RoundingMode.HALF_UP) + ")"
                );
                linea.getStyle().set("font-size", "0.9rem");
                contenido.add(linea);

                if (d.getNotaCocina() != null && !d.getNotaCocina().isBlank()) {
                    Span nota = new Span("Nota: " + d.getNotaCocina());
                    nota.getStyle()
                            .set("font-size", "0.8rem")
                            .set("font-style", "italic")
                            .set("color", "var(--lumo-secondary-text-color)");
                    contenido.add(nota);
                }
            }
        } else {
            contenido.add(new Span("(Sin detalle de productos)"));
        }

        Button cerrar = new Button("Cerrar", e -> dialog.close());
        cerrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout wrapper = new VerticalLayout(contenido, cerrar);
        wrapper.setPadding(false);
        wrapper.setSpacing(true);
        wrapper.setAlignItems(FlexComponent.Alignment.END);

        dialog.add(wrapper);
        dialog.open();
    }

    // =========================
    // Cambio de estado
    // =========================
    private void cambiarEstado(Venta venta, String nuevoEstado) {
        venta.setEstadoCocina(nuevoEstado);
        ventaRepository.save(venta);
        cargarOrdenesHoy();
    }
}
