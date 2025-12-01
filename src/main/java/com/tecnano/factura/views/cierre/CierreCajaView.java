package com.tecnano.factura.views.cierre;

import com.tecnano.factura.model.Venta;
import com.tecnano.factura.repository.VentaRepository;
import com.tecnano.factura.views.MainLayout;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Route(value = "cierre-caja", layout = MainLayout.class)
@PageTitle("Cierre de caja")
@PermitAll
public class CierreCajaView extends VerticalLayout {

    private final VentaRepository ventaRepository;

    private final DatePicker fechaPicker = new DatePicker("Fecha de cierre");
    private final Span ventasSpan = new Span("0");
    private final Span efectivoSpan = new Span("$ 0.00");
    private final Span tarjetaSpan = new Span("$ 0.00");
    private final Span transferenciaSpan = new Span("$ 0.00");
    private final Span totalSpan = new Span("$ 0.00");

    // texto "Efectivo según sistema"
    private final Span esperadoEfectivoSpan = new Span("$ 0.00");

    private final NumberField efectivoContadoField = new NumberField("Efectivo contado");
    private final Span diferenciaSpan = new Span("$ 0.00");

    private BigDecimal efectivoSistema = BigDecimal.ZERO;

    public CierreCajaView(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H1 titulo = new H1("Cierre de caja");

        fechaPicker.setValue(LocalDate.now());
        fechaPicker.addValueChangeListener(e -> cargarCierre());

        ventasSpan.getStyle().set("font-weight", "700");
        efectivoSpan.getStyle().set("font-weight", "700");
        tarjetaSpan.getStyle().set("font-weight", "700");
        transferenciaSpan.getStyle().set("font-weight", "700");
        totalSpan.getStyle().set("font-weight", "700");
        diferenciaSpan.getStyle().set("font-weight", "700");

        efectivoContadoField.setMin(0);
        efectivoContadoField.setPlaceholder("Monto contado en caja");
        efectivoContadoField.addValueChangeListener(e -> recalcularDiferencia());

        VerticalLayout resumenLayout = crearResumenLayout();
        VerticalLayout conteoLayout = crearConteoLayout();

        HorizontalLayout contenido = new HorizontalLayout(resumenLayout, conteoLayout);
        contenido.setWidthFull();
        contenido.setSpacing(true);
        contenido.setAlignItems(FlexComponent.Alignment.START);

        // Usamos el layout principal; sin MainMenu aquí
        add(titulo, fechaPicker, contenido);

        cargarCierre();
    }

    private VerticalLayout crearResumenLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidth("400px");
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.addClassName("card-blanca");   // usamos estilo global de tarjeta

        layout.add(
                crearLinea("Ventas del día:", ventasSpan),
                crearLinea("Total efectivo:", efectivoSpan),
                crearLinea("Total tarjeta:", tarjetaSpan),
                crearLinea("Total transferencia:", transferenciaSpan),
                new Span("-------------------------"),
                crearLinea("TOTAL:", totalSpan)
        );

        return layout;
    }

    private VerticalLayout crearConteoLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidth("400px");
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.addClassName("card-blanca");   // usamos estilo global de tarjeta

        Span titulo = new Span("Conteo de efectivo");
        titulo.getStyle().set("font-weight", "600");

        Span esperadoLabel = new Span("Efectivo según sistema (solo efectivo):");
        esperadoLabel.getStyle().set("font-size", "0.85rem");

        esperadoEfectivoSpan.getStyle().set("font-weight", "600");

        VerticalLayout esperadoLayout = new VerticalLayout(esperadoLabel, esperadoEfectivoSpan);
        esperadoLayout.setPadding(false);
        esperadoLayout.setSpacing(false);

        diferenciaSpan.getStyle().set("font-size", "1rem");

        layout.add(
                titulo,
                esperadoLayout,
                efectivoContadoField,
                crearLinea("Diferencia:", diferenciaSpan)
        );

        return layout;
    }

    private HorizontalLayout crearLinea(String etiqueta, Span valor) {
        Span label = new Span(etiqueta);
        label.getStyle().set("font-size", "0.9rem");

        HorizontalLayout hl = new HorizontalLayout(label, valor);
        hl.setWidthFull();
        hl.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        hl.setAlignItems(FlexComponent.Alignment.CENTER);
        return hl;
    }

    private void cargarCierre() {
        LocalDate fecha = fechaPicker.getValue();
        if (fecha == null) {
            Notification.show("Selecciona una fecha");
            return;
        }

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay().minusNanos(1);

        List<Venta> ventasDia = ventaRepository.findByFechaHoraBetween(desde, hasta);

        int numVentas = ventasDia.size();
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTarjeta = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;

        for (Venta v : ventasDia) {
            if (v.getTotal() == null) continue;
            String forma = v.getFormaPago() != null ? v.getFormaPago() : "Efectivo";
            switch (forma.toLowerCase()) {
                case "tarjeta" -> totalTarjeta = totalTarjeta.add(v.getTotal());
                case "transferencia" -> totalTransferencia = totalTransferencia.add(v.getTotal());
                default -> totalEfectivo = totalEfectivo.add(v.getTotal());
            }
        }

        BigDecimal totalGeneral = totalEfectivo.add(totalTarjeta).add(totalTransferencia);

        ventasSpan.setText(String.valueOf(numVentas));
        efectivoSpan.setText("$ " + totalEfectivo.setScale(2, RoundingMode.HALF_UP));
        tarjetaSpan.setText("$ " + totalTarjeta.setScale(2, RoundingMode.HALF_UP));
        transferenciaSpan.setText("$ " + totalTransferencia.setScale(2, RoundingMode.HALF_UP));
        totalSpan.setText("$ " + totalGeneral.setScale(2, RoundingMode.HALF_UP));

        // este es el efectivo que el sistema espera
        efectivoSistema = totalEfectivo;
        esperadoEfectivoSpan.setText("$ " + efectivoSistema.setScale(2, RoundingMode.HALF_UP));

        // recalculamos diferencia con lo que haya digitado la señora (si ya puso algo)
        recalcularDiferencia();
    }

    private void recalcularDiferencia() {
        Double contadoValor = efectivoContadoField.getValue();
        BigDecimal contado = contadoValor != null
                ? BigDecimal.valueOf(contadoValor)
                : BigDecimal.ZERO;

        BigDecimal diferencia = contado.subtract(efectivoSistema).setScale(2, RoundingMode.HALF_UP);
        diferenciaSpan.setText("$ " + diferencia.toString());
        if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
            diferenciaSpan.getStyle().set("color", "var(--lumo-success-text-color)");
        } else {
            diferenciaSpan.getStyle().set("color", "var(--lumo-error-text-color)");
        }
    }
}
