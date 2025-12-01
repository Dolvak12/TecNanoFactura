package com.tecnano.factura.views.ventas;

import com.tecnano.factura.model.Venta;
import com.tecnano.factura.repository.VentaRepository;
import com.tecnano.factura.service.SriService;
import com.tecnano.factura.ui.Notificaciones;
import com.tecnano.factura.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Route(value = "ventas-pendientes-sri", layout = MainLayout.class)
@PageTitle("Ventas pendientes SRI")
@PermitAll
public class VentasPendientesSriView extends VerticalLayout {

    private final VentaRepository ventaRepository;
    private final SriService sriService;

    private final Grid<Venta> grid = new Grid<>(Venta.class, false);

    private final DateTimeFormatter fechaHoraFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public VentasPendientesSriView(VentaRepository ventaRepository,
                                   SriService sriService) {
        this.ventaRepository = ventaRepository;
        this.sriService = sriService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        construirUI();
        cargarVentas();
    }

    // =========================
    // UI
    // =========================
    private void construirUI() {
        H1 titulo = new H1("Ventas pendientes / con error SRI");
        titulo.getStyle().set("margin-bottom", "0.25rem");

        Span ayuda = new Span(
                "Aquí ves las ventas con estado SRI = PENDIENTE o ERROR. " +
                        "Puedes reintentar el envío, revisar el mensaje de error o abrir el RIDE si existe."
        );
        ayuda.getStyle()
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(titulo, ayuda);
        header.setPadding(false);
        header.setSpacing(false);

        configurarGrid();

        Button refrescarBtn = new Button("Refrescar", e -> cargarVentas());
        refrescarBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout barraAcciones = new HorizontalLayout(refrescarBtn);
        barraAcciones.setWidthFull();
        barraAcciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout card = new VerticalLayout(barraAcciones, grid);
        card.addClassName("card-blanca");
        card.setPadding(true);
        card.setSpacing(true);
        card.setWidthFull();

        add(header, card);
    }

    private void configurarGrid() {
        grid.addColumn(Venta::getId)
                .setHeader("N°")
                .setAutoWidth(true);

        grid.addColumn(v -> {
                    if (v.getFechaHora() == null) return "";
                    return v.getFechaHora().format(fechaHoraFormatter);
                }).setHeader("Fecha/Hora")
                .setAutoWidth(true);

        grid.addColumn(v -> {
                    String ubic = v.getUbicacion() != null ? v.getUbicacion() : v.getMesa();
                    return ubic != null ? ubic : "";
                }).setHeader("Mesa/Pedido")
                .setFlexGrow(1);

        grid.addColumn(v -> {
                    BigDecimal t = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
                    return t.setScale(2, RoundingMode.HALF_UP);
                }).setHeader("Total")
                .setAutoWidth(true);

        grid.addColumn(v -> normalizar(v.getEstadoSri()))
                .setHeader("Estado SRI")
                .setAutoWidth(true);

        grid.addColumn(v -> {
                    String msg = v.getMensajeErrorSri();
                    if (msg == null || msg.isBlank()) return "";
                    return msg.length() > 60 ? msg.substring(0, 57) + "..." : msg;
                }).setHeader("Mensaje error (resumen)")
                .setFlexGrow(1);

        grid.addComponentColumn(this::crearAcciones)
                .setHeader("Acciones")
                .setAutoWidth(true);

        grid.setHeight("500px");
    }

    private HorizontalLayout crearAcciones(Venta venta) {
        Button reenviarBtn = new Button("Reintentar SRI", e -> reintentarSri(venta));
        reenviarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        Button verErrorBtn = new Button("Ver error", e -> mostrarErrorSri(venta));
        verErrorBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        verErrorBtn.setEnabled(
                venta.getMensajeErrorSri() != null && !venta.getMensajeErrorSri().isBlank()
        );

        Button verRideBtn = new Button("Ver RIDE", e ->
                UI.getCurrent().getPage()
                        .open("/api/ventas/" + venta.getId() + "/ride", "_blank")
        );
        verRideBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);

        HorizontalLayout hl = new HorizontalLayout(reenviarBtn, verErrorBtn, verRideBtn);
        hl.setSpacing(true);
        return hl;
    }

    // =========================
    // Datos
    // =========================
    private void cargarVentas() {
        List<Venta> todas = ventaRepository.findAll();

        List<Venta> filtradas = todas.stream()
                .filter(v -> {
                    String est = normalizar(v.getEstadoSri());
                    return "PENDIENTE".equals(est) || "ERROR".equals(est);
                })
                .collect(Collectors.toList());

        grid.setItems(filtradas);

        Notificaciones.info("Cargadas " + filtradas.size() + " ventas pendientes / con error.");
    }

    // =========================
    // Acciones SRI
    // =========================
    private void reintentarSri(Venta venta) {
        try {
            sriService.procesarVentaConSri(venta);
            ventaRepository.save(venta);

            String estadoFinal = normalizar(venta.getEstadoSri());
            if ("AUTORIZADA".equals(estadoFinal)) {
                Notificaciones.exito("Venta " + venta.getId() + " AUTORIZADA por SRI.");
            } else if ("ERROR".equals(estadoFinal)) {
                Notificaciones.advertencia(
                        "Venta " + venta.getId() + " sigue en ERROR. Revisa el mensaje."
                );
            } else {
                Notificaciones.info(
                        "Venta " + venta.getId() + " quedó en estado: " + estadoFinal
                );
            }

            cargarVentas();
        } catch (Exception ex) {
            Notificaciones.error("Error al reintentar SRI: " +
                    (ex.getMessage() != null ? ex.getMessage() : "Desconocido"));
            ex.printStackTrace();
        }
    }

    private void mostrarErrorSri(Venta venta) {
        String msg = venta.getMensajeErrorSri();
        if (msg == null || msg.isBlank()) {
            Notificaciones.info("Esta venta no tiene mensaje de error registrado.");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Error SRI - Venta #" + venta.getId());

        Span texto = new Span(msg);
        texto.getStyle().set("white-space", "pre-wrap");

        Button cerrar = new Button("Cerrar", e -> dialog.close());
        cerrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout contenido = new VerticalLayout(texto, cerrar);
        contenido.setPadding(false);
        contenido.setSpacing(true);
        contenido.setAlignItems(FlexComponent.Alignment.END);

        dialog.add(contenido);
        dialog.open();
    }

    private String normalizar(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase();
    }
}
