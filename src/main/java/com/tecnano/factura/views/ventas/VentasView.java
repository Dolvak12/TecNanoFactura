package com.tecnano.factura.views.ventas;

import com.tecnano.factura.model.Venta;
import com.tecnano.factura.repository.VentaRepository;
import com.tecnano.factura.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Route(value = "ventas", layout = MainLayout.class)
@PageTitle("Ventas")
@PermitAll
public class VentasView extends VerticalLayout {

    private final VentaRepository ventaRepository;

    private final DatePicker desdePicker = new DatePicker("Desde");
    private final DatePicker hastaPicker = new DatePicker("Hasta");
    private final Grid<Venta> grid = new Grid<>(Venta.class, false);

    // Resumen
    private final Span totalVentasSpan = new Span("0");
    private final Span totalMontoSpan = new Span("$ 0.00");
    private final Span efectivoSpan = new Span("$ 0.00");
    private final Span tarjetaSpan = new Span("$ 0.00");
    private final Span transferenciaSpan = new Span("$ 0.00");

    // “Gráfico” simple por forma de pago
    private final VerticalLayout graficoVentasLayout = new VerticalLayout();

    public VentasView(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        construirUI();
        configurarGrid();
        configurarEventos();
        cargarVentasIniciales();
    }

    // =========================
    // UI
    // =========================
    private void construirUI() {
        H1 titulo = new H1("Ventas");
        titulo.getStyle().set("margin-bottom", "0.25rem");

        Span ayuda = new Span("Consulta las ventas por rango de fechas y revisa el resumen por forma de pago.");
        ayuda.getStyle()
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(titulo, ayuda);
        header.setPadding(false);
        header.setSpacing(false);

        // Filtros de fecha
        desdePicker.setClearButtonVisible(true);
        hastaPicker.setClearButtonVisible(true);

        Button hoyBtn = new Button("Hoy", e -> setRangoHoy());
        hoyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button semanaBtn = new Button("Esta semana", e -> setRangoSemanaActual());
        semanaBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button buscarBtn = new Button("Buscar", e -> cargarVentasEnRango());
        buscarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout filtros = new HorizontalLayout(desdePicker, hastaPicker, hoyBtn, semanaBtn, buscarBtn);
        filtros.setWidthFull();
        filtros.setAlignItems(Alignment.END);
        filtros.setSpacing(true);

        // Cards: resumen y “gráfico”
        VerticalLayout resumenCard = crearResumenCard();
        VerticalLayout graficoCard = crearGraficoCard();

        HorizontalLayout filaSuperior = new HorizontalLayout(resumenCard, graficoCard);
        filaSuperior.setWidthFull();
        filaSuperior.setSpacing(true);
        filaSuperior.setAlignItems(Alignment.START);
        filaSuperior.setFlexGrow(1, resumenCard, graficoCard);

        // Card con el grid
        Div cardGrid = new Div(grid);
        cardGrid.addClassName("card-blanca");
        cardGrid.getStyle().set("margin-top", "0.5rem");
        cardGrid.setWidthFull();

        add(header, filtros, filaSuperior, cardGrid);
    }

    private VerticalLayout crearResumenCard() {
        VerticalLayout layout = new VerticalLayout();
        layout.addClassName("card-blanca");
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();

        Span titulo = new Span("Resumen de ventas");
        titulo.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.95rem");

        totalVentasSpan.getStyle().set("font-weight", "700");
        totalMontoSpan.getStyle().set("font-weight", "700");
        efectivoSpan.getStyle().set("font-weight", "600");
        tarjetaSpan.getStyle().set("font-weight", "600");
        transferenciaSpan.getStyle().set("font-weight", "600");

        layout.add(
                titulo,
                crearLineaResumen("N° ventas:", totalVentasSpan),
                crearLineaResumen("Total vendido:", totalMontoSpan),
                crearLineaResumen("Total efectivo:", efectivoSpan),
                crearLineaResumen("Total tarjeta:", tarjetaSpan),
                crearLineaResumen("Total transferencia:", transferenciaSpan)
        );

        return layout;
    }

    private VerticalLayout crearGraficoCard() {
        VerticalLayout layout = new VerticalLayout();
        layout.addClassName("card-blanca");         // 👈 panel del gráfico con card-blanca
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();

        Span titulo = new Span("Distribución por forma de pago");
        titulo.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.95rem");

        graficoVentasLayout.setPadding(false);
        graficoVentasLayout.setSpacing(false);
        graficoVentasLayout.setWidthFull();

        layout.add(titulo, graficoVentasLayout);
        return layout;
    }

    private HorizontalLayout crearLineaResumen(String etiqueta, Span valor) {
        Span label = new Span(etiqueta);
        label.getStyle().set("font-size", "0.9rem");

        valor.getStyle().set("font-size", "0.9rem");

        HorizontalLayout hl = new HorizontalLayout(label, valor);
        hl.setWidthFull();
        hl.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        hl.setAlignItems(Alignment.CENTER);
        return hl;
    }

    // =========================
    // Grid
    // =========================
    private void configurarGrid() {
        grid.addColumn(Venta::getId)
                .setHeader("N°")
                .setAutoWidth(true);

        grid.addColumn(v -> v.getFechaHora() != null ? v.getFechaHora().toString() : "")
                .setHeader("Fecha/Hora")
                .setAutoWidth(true);

        grid.addColumn(v -> {
                    String ubic = v.getUbicacion() != null ? v.getUbicacion() : v.getMesa();
                    return ubic != null ? ubic : "";
                }).setHeader("Mesa/Pedido")
                .setFlexGrow(1);

        grid.addColumn(Venta::getFormaPago)
                .setHeader("Forma de pago")
                .setAutoWidth(true);

        grid.addColumn(v -> {
                    BigDecimal t = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
                    return t.setScale(2, RoundingMode.HALF_UP);
                }).setHeader("Total")
                .setAutoWidth(true);

        grid.setHeight("420px");
    }

    // =========================
    // Eventos / filtros
    // =========================
    private void configurarEventos() {
        // Solo actualizamos cuando el usuario da a "Buscar"
        // pero si quieres, puedes enganchar valueChangeListeners aquí.
    }

    private void cargarVentasIniciales() {
        LocalDate hoy = LocalDate.now();
        desdePicker.setValue(hoy);
        hastaPicker.setValue(hoy);
        cargarVentasEnRango();
    }

    private void setRangoHoy() {
        LocalDate hoy = LocalDate.now();
        desdePicker.setValue(hoy);
        hastaPicker.setValue(hoy);
        cargarVentasEnRango();
    }

    private void setRangoSemanaActual() {
        LocalDate hoy = LocalDate.now();
        DayOfWeek dow = hoy.getDayOfWeek();
        int diff = dow.getValue() - DayOfWeek.MONDAY.getValue(); // lunes = 1
        LocalDate lunes = hoy.minusDays(diff);
        LocalDate domingo = lunes.plusDays(6);

        desdePicker.setValue(lunes);
        hastaPicker.setValue(domingo);
        cargarVentasEnRango();
    }

    private void cargarVentasEnRango() {
        LocalDate desde = desdePicker.getValue();
        LocalDate hasta = hastaPicker.getValue();

        if (desde == null || hasta == null) {
            Notification.show("Selecciona el rango de fechas (desde / hasta).");
            return;
        }
        if (hasta.isBefore(desde)) {
            Notification.show("La fecha 'Hasta' no puede ser anterior a 'Desde'.");
            return;
        }

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay().minusNanos(1);

        List<Venta> ventas = ventaRepository.findByFechaHoraBetween(inicio, fin);

        grid.setItems(ventas);
        actualizarResumen(ventas);
    }

    // =========================
    // Resumen + “gráfico”
    // =========================
    private void actualizarResumen(List<Venta> ventas) {
        int num = ventas.size();
        BigDecimal totalMonto = BigDecimal.ZERO;
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTarjeta = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;

        for (Venta v : ventas) {
            BigDecimal t = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
            totalMonto = totalMonto.add(t);

            String forma = v.getFormaPago() != null ? v.getFormaPago().toLowerCase() : "efectivo";
            switch (forma) {
                case "tarjeta" -> totalTarjeta = totalTarjeta.add(t);
                case "transferencia" -> totalTransferencia = totalTransferencia.add(t);
                default -> totalEfectivo = totalEfectivo.add(t);
            }
        }

        totalVentasSpan.setText(String.valueOf(num));
        totalMontoSpan.setText("$ " + totalMonto.setScale(2, RoundingMode.HALF_UP));
        efectivoSpan.setText("$ " + totalEfectivo.setScale(2, RoundingMode.HALF_UP));
        tarjetaSpan.setText("$ " + totalTarjeta.setScale(2, RoundingMode.HALF_UP));
        transferenciaSpan.setText("$ " + totalTransferencia.setScale(2, RoundingMode.HALF_UP));

        actualizarGraficoTotales(totalEfectivo, totalTarjeta, totalTransferencia);
    }

    private void actualizarGraficoTotales(BigDecimal efectivo,
                                          BigDecimal tarjeta,
                                          BigDecimal transferencia) {
        graficoVentasLayout.removeAll();

        BigDecimal max = efectivo.max(tarjeta).max(transferencia);
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            Span sinDatos = new Span("Sin datos para mostrar el gráfico.");
            sinDatos.getStyle()
                    .set("font-size", "0.8rem")
                    .set("color", "var(--lumo-secondary-text-color)");
            graficoVentasLayout.add(sinDatos);
            return;
        }

        HorizontalLayout barras = new HorizontalLayout();
        barras.setWidthFull();
        barras.setSpacing(true);
        barras.setAlignItems(Alignment.END);

        barras.add(
                crearBarra("Efectivo", efectivo, max),
                crearBarra("Tarjeta", tarjeta, max),
                crearBarra("Transf.", transferencia, max)
        );

        graficoVentasLayout.add(barras);
    }

    private VerticalLayout crearBarra(String label,
                                      BigDecimal valor,
                                      BigDecimal max) {
        double porcentaje = max.compareTo(BigDecimal.ZERO) > 0
                ? valor.divide(max, 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        int alturaPx = (int) (40 + porcentaje * 100); // altura mínima 40px

        Div barra = new Div();
        barra.getStyle()
                .set("width", "40px")
                .set("height", alturaPx + "px")
                .set("border-radius", "12px")
                .set("background", "var(--tecnano-primary, var(--lumo-primary-color))")
                .set("opacity", valor.compareTo(BigDecimal.ZERO) > 0 ? "1" : "0.3");

        Span valorSpan = new Span("$ " + valor.setScale(2, RoundingMode.HALF_UP));
        valorSpan.getStyle()
                .set("font-size", "0.75rem")
                .set("text-align", "center");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "0.75rem")
                .set("text-align", "center");

        VerticalLayout wrapper = new VerticalLayout(valorSpan, barra, labelSpan);
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setAlignItems(Alignment.CENTER);

        return wrapper;
    }
}
