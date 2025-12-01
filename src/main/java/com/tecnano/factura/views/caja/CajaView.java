package com.tecnano.factura.views.caja;

import com.tecnano.factura.model.Cliente;
import com.tecnano.factura.model.Producto;
import com.tecnano.factura.model.Venta;
import com.tecnano.factura.repository.ProductoRepository;
import com.tecnano.factura.repository.VentaRepository;
import com.tecnano.factura.service.ImpresionCocinaService;
import com.tecnano.factura.service.VentaService;
import com.tecnano.factura.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PageTitle("Caja")
@Route(value = "caja", layout = MainLayout.class)
@PermitAll
public class CajaView extends VerticalLayout {

    // === Dependencias ===
    private final ProductoRepository productoRepository;
    private final VentaService ventaService;
    private final VentaRepository ventaRepository;
    private final ImpresionCocinaService impresionCocinaService;

    // === Estado UI / Datos en memoria ===
    private final List<LineaUI> lineas = new ArrayList<>();

    // === Componentes UI ===
    private final Grid<LineaUI> detalleGrid = new Grid<>(LineaUI.class, false);
    private final VerticalLayout platosLayout = new VerticalLayout();
    private final TextField filtroPlatos = new TextField();

    private final Select<String> formaPago = new Select<>();
    private final Select<String> tipoCliente = new Select<>();
    private final Select<String> tipoDocumento = new Select<>();
    private final Select<String> ubicacionField = new Select<>();

    private final TextField numeroDocumento = new TextField("Documento");
    private final TextField nombreCliente = new TextField("Nombre/Razón social");
    private final TextField correoCliente = new TextField("Correo");

    private final NumberField efectivoField = new NumberField("Efectivo");
    private final NumberField cambioField = new NumberField("Cambio");

    private final Span totalSpan = new Span("0.00");
    private final Span totalDiaSpan = new Span("0.00");

    private final Button cobrarBtn = new Button("Cobrar");
    private final Button limpiarBtn = new Button("Limpiar");

    // === Constructor ===
    public CajaView(ProductoRepository productoRepository,
                    VentaService ventaService,
                    VentaRepository ventaRepository,
                    ImpresionCocinaService impresionCocinaService) {
        this.productoRepository = productoRepository;
        this.ventaService = ventaService;
        this.ventaRepository = ventaRepository;
        this.impresionCocinaService = impresionCocinaService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        construirUI();
        configurarEventos();
        actualizarEstadoDelDia();
        actualizarEstadoBotonCobrar();
    }

    // =========================
    // Construcción de la interfaz
    // =========================
    private void construirUI() {
        add(crearBarraSuperior());

        // COLUMNA IZQUIERDA: PLATOS
        VerticalLayout columnaIzquierda = crearColumnaPlatos();
        Div cardIzquierda = new Div(columnaIzquierda);
        cardIzquierda.addClassName("card-blanca");

        // COLUMNA DERECHA: DETALLE + CLIENTE + PAGO + TOTAL
        VerticalLayout contenidoDerecha = crearColumnaDerecha();
        Div cardDerecha = new Div(contenidoDerecha);
        cardDerecha.addClassName("card-blanca");
        cardDerecha.getStyle().set("margin-top", "0.5rem");

        HorizontalLayout contenido = new HorizontalLayout(cardIzquierda, cardDerecha);
        contenido.setWidthFull();
        contenido.setSpacing(true);
        contenido.setAlignItems(FlexComponent.Alignment.START);
        contenido.setFlexGrow(0, cardIzquierda);
        contenido.setFlexGrow(1, cardDerecha);

        add(contenido);
    }

    private HorizontalLayout crearBarraSuperior() {
        ubicacionField.setLabel("Mesa / pedido");
        ubicacionField.setItems(
                "Mostrador",
                "Mesa 1", "Mesa 2", "Mesa 3", "Mesa 4", "Mesa 5", "Mesa 6", "Mesa 7", "Mesa 8", "Mesa 9",
                "Para llevar",
                "Domicilio"
        );
        ubicacionField.setPlaceholder("Elige mesa / pedido");
        ubicacionField.setWidth("220px");

        Span totalDiaLabel = new Span("Total del día:");
        totalDiaLabel.getStyle()
                .set("font-size", "0.85rem")
                .set("color", "var(--lumo-secondary-text-color)");

        totalDiaSpan.getStyle()
                .set("font-weight", "600")
                .set("margin-left", "0.25rem");

        HorizontalLayout totalDiaLayout = new HorizontalLayout(totalDiaLabel, totalDiaSpan);
        totalDiaLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout top = new HorizontalLayout(ubicacionField, totalDiaLayout);
        top.setAlignItems(FlexComponent.Alignment.END);
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return top;
    }

    private VerticalLayout crearColumnaPlatos() {
        H3 platosTitulo = new H3("Platos y bebidas");
        platosTitulo.getStyle()
                .set("margin-top", "0")
                .set("margin-bottom", "0.25rem");

        // Buscador
        filtroPlatos.setPlaceholder("Buscar plato o bebida...");
        filtroPlatos.setClearButtonVisible(true);
        filtroPlatos.setWidthFull();
        filtroPlatos.setPrefixComponent(VaadinIcon.SEARCH.create());
        filtroPlatos.addValueChangeListener(e -> filtrarPlatos());

        // Contenedor de botones de platos
        platosLayout.setPadding(false);
        platosLayout.setSpacing(true);
        platosLayout.addClassName("platos-list");

        // Carga inicial sin filtro
        cargarCatalogoPlatos(null);

        VerticalLayout columnaIzquierda = new VerticalLayout(platosTitulo, filtroPlatos, platosLayout);
        columnaIzquierda.setPadding(false);
        columnaIzquierda.setSpacing(true);
        columnaIzquierda.setWidth("340px");

        return columnaIzquierda;
    }

    private VerticalLayout crearColumnaDerecha() {
        configurarGridDetalle();

        // --- Sección Cliente ---
        Span tituloCliente = crearTituloSeccion("Datos del cliente");

        tipoCliente.setLabel("Tipo de cliente");
        tipoCliente.setItems("Consumidor final", "Con datos");
        tipoCliente.setValue("Consumidor final");

        tipoDocumento.setLabel("Tipo documento");
        tipoDocumento.setItems("Cédula", "RUC", "Pasaporte");

        numeroDocumento.setClearButtonVisible(true);
        nombreCliente.setClearButtonVisible(true);
        correoCliente.setClearButtonVisible(true);

        HorizontalLayout filaCliente1 = new HorizontalLayout(tipoCliente, tipoDocumento, numeroDocumento);
        filaCliente1.setWidthFull();
        filaCliente1.setFlexGrow(1, numeroDocumento);

        HorizontalLayout filaCliente2 = new HorizontalLayout(nombreCliente, correoCliente);
        filaCliente2.setWidthFull();
        filaCliente2.setFlexGrow(1, nombreCliente);

        // --- Sección Pago ---
        Span tituloPago = crearTituloSeccion("Forma de pago");

        formaPago.setLabel("Forma de pago");
        formaPago.setItems("Efectivo", "Tarjeta", "Transferencia");
        formaPago.setValue("Efectivo");

        efectivoField.setStep(0.01);
        efectivoField.setMin(0);
        cambioField.setStep(0.01);
        cambioField.setReadOnly(true);

        HorizontalLayout filaPago = new HorizontalLayout(formaPago, efectivoField, cambioField);
        filaPago.setAlignItems(FlexComponent.Alignment.END);

        // --- TOTAL + BOTONES ---
        cobrarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cobrarBtn.addClassName("boton-cobrar");

        limpiarBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Span totalTitle = new Span("TOTAL A PAGAR");
        totalTitle.getStyle()
                .set("display", "block")
                .set("font-weight", "600")
                .set("font-size", "0.85rem")
                .set("margin-bottom", "0.25rem");

        totalSpan.getStyle()
                .set("font-size", "1.6rem")
                .set("font-weight", "700");

        Div totalCard = new Div(totalTitle, totalSpan);
        totalCard.addClassName("card-total");

        HorizontalLayout accionesPie = new HorizontalLayout(cobrarBtn, limpiarBtn);
        accionesPie.setWidthFull();
        accionesPie.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        accionesPie.setSpacing(true);

        VerticalLayout contenidoDerecha = new VerticalLayout(
                detalleGrid,
                tituloCliente,
                filaCliente1,
                filaCliente2,
                tituloPago,
                filaPago,
                totalCard,
                accionesPie
        );
        contenidoDerecha.setPadding(false);
        contenidoDerecha.setSpacing(true);
        contenidoDerecha.setWidthFull();

        return contenidoDerecha;
    }

    private void configurarGridDetalle() {
        detalleGrid.addColumn(l -> l.getProducto().getCodigo())
                .setHeader("Código").setAutoWidth(true);
        detalleGrid.addColumn(l -> l.getProducto().getNombre())
                .setHeader("Producto").setFlexGrow(1);
        detalleGrid.addColumn(LineaUI::getCantidad)
                .setHeader("Cant.").setAutoWidth(true);
        detalleGrid.addColumn(l -> l.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP))
                .setHeader("P.Unit").setAutoWidth(true);
        detalleGrid.addColumn(l -> l.getSubtotal().setScale(2, RoundingMode.HALF_UP))
                .setHeader("Subtotal").setAutoWidth(true);
        detalleGrid.addColumn(LineaUI::getNotaCocina)
                .setHeader("Nota").setFlexGrow(1);

        detalleGrid.addComponentColumn(this::crearAccionesLinea)
                .setHeader("Acciones")
                .setAutoWidth(true);

        detalleGrid.setItems(lineas);
        detalleGrid.setWidthFull();
        detalleGrid.setHeight("350px");
    }

    private Span crearTituloSeccion(String texto) {
        Span s = new Span(texto);
        s.getStyle()
                .set("font-weight", "600")
                .set("margin-top", "0.5rem")
                .set("margin-bottom", "0.15rem")
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-secondary-text-color)");
        return s;
    }

    // Aplica el filtro del TextField
    private void filtrarPlatos() {
        String term = filtroPlatos.getValue();
        if (term == null || term.isBlank()) {
            cargarCatalogoPlatos(null);
        } else {
            cargarCatalogoPlatos(term.trim().toLowerCase());
        }
    }

    // Crea los botones de platos (catálogo)
    private void cargarCatalogoPlatos(String filtro) {
        platosLayout.removeAll();

        List<Producto> productos = productoRepository.findAll();
        if (filtro != null && !filtro.isBlank()) {
            String t = filtro.toLowerCase();
            productos = productos.stream()
                    .filter(p ->
                            (p.getNombre() != null && p.getNombre().toLowerCase().contains(t)) ||
                                    (p.getCodigo() != null && p.getCodigo().toLowerCase().contains(t))
                    )
                    .toList();
        }

        if (productos.isEmpty()) {
            Span vacio = new Span("No se encontraron platos.");
            vacio.getStyle()
                    .set("font-size", "0.85rem")
                    .set("color", "var(--lumo-secondary-text-color)");
            platosLayout.add(vacio);
            return;
        }

        for (Producto p : productos) {
            String emoji = emojiParaProducto(p.getNombre());
            String texto = emoji + " " + p.getNombre()
                    + "  $" + p.getPrecio().setScale(2, RoundingMode.HALF_UP);

            Button botonPlato = new Button(texto);
            botonPlato.addClassName("plato-boton");
            botonPlato.setWidthFull();

            botonPlato.addClickListener(e -> {
                agregarOIncrementarLinea(p);
                refrescarDespuesDeCambioDeLineas();
            });

            platosLayout.add(botonPlato);
        }
    }

    private String emojiParaProducto(String nombre) {
        String n = Optional.ofNullable(nombre).orElse("").toLowerCase();
        if (n.contains("hornado") || n.contains("carne") || n.contains("guata")) return "🍽";
        if (n.contains("seco") || n.contains("pollo")) return "🍗";
        if (n.contains("caldo") || n.contains("encebollado")) return "🍲";
        if (n.contains("arroz") || n.contains("mote") || n.contains("chaulafán") || n.contains("chaulafan")) return "🍚";
        if (n.contains("cola") || n.contains("coca")) return "🥤";
        return "🍽";
    }

    // Columna de acciones de cada línea
    private HorizontalLayout crearAccionesLinea(LineaUI linea) {
        Button menosBtn = new Button("−", e -> {
            if (linea.getCantidad() > 1) {
                linea.setCantidad(linea.getCantidad() - 1);
            } else {
                lineas.remove(linea);
            }
            refrescarDespuesDeCambioDeLineas();
        });
        menosBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        Button masBtn = new Button("+", e -> {
            linea.setCantidad(linea.getCantidad() + 1);
            refrescarDespuesDeCambioDeLineas();
        });
        masBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        Button notaBtn = new Button("📝", e -> editarNota(linea));
        notaBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        Button borrarBtn = new Button("🗑️", e -> {
            lineas.remove(linea);
            refrescarDespuesDeCambioDeLineas();
        });
        borrarBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout acciones = new HorizontalLayout(menosBtn, masBtn, notaBtn, borrarBtn);
        acciones.setSpacing(false);
        acciones.getStyle().set("gap", "0.2rem");
        return acciones;
    }

    private void editarNota(LineaUI linea) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nota para cocina");

        TextArea notaArea = new TextArea("Nota");
        notaArea.setWidthFull();
        notaArea.setValue(Optional.ofNullable(linea.getNotaCocina()).orElse(""));

        Button guardar = new Button("Guardar", e -> {
            linea.setNotaCocina(notaArea.getValue());
            actualizarDetalle();
            dialog.close();
        });
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelar = new Button("Cancelar", e -> dialog.close());

        HorizontalLayout acciones = new HorizontalLayout(guardar, cancelar);
        acciones.setSpacing(true);

        VerticalLayout contenido = new VerticalLayout(notaArea, acciones);
        contenido.setPadding(false);
        contenido.setSpacing(true);

        dialog.add(contenido);
        dialog.open();
    }

    // =========================
    // Eventos generales
    // =========================
    private void configurarEventos() {
        formaPago.addValueChangeListener(e -> {
            boolean esEfectivo = "Efectivo".equals(e.getValue());
            efectivoField.setEnabled(esEfectivo);
            if (!esEfectivo) {
                efectivoField.clear();
                cambioField.clear();
            }
            actualizarEstadoBotonCobrar();
        });

        efectivoField.addValueChangeListener(e -> {
            try {
                BigDecimal total = new BigDecimal(totalSpan.getText());
                Double ef = e.getValue();
                if (ef == null) {
                    cambioField.clear();
                    return;
                }
                BigDecimal cambio = BigDecimal.valueOf(ef).subtract(total);
                if (cambio.signum() < 0) {
                    cambioField.setValue(0d);
                } else {
                    cambioField.setValue(
                            cambio.setScale(2, RoundingMode.HALF_UP).doubleValue()
                    );
                }
            } catch (Exception ignored) {}
        });

        tipoCliente.addValueChangeListener(e -> {
            boolean conDatos = "Con datos".equals(e.getValue());
            tipoDocumento.setEnabled(conDatos);
            numeroDocumento.setEnabled(conDatos);
            nombreCliente.setEnabled(conDatos);
            correoCliente.setEnabled(conDatos);
            if (!conDatos) {
                tipoDocumento.clear();
                numeroDocumento.clear();
                nombreCliente.clear();
                correoCliente.clear();
            }
        });

        cobrarBtn.addClickListener(e -> cobrarReal());
        limpiarBtn.addClickListener(e -> limpiarVenta());
    }

    // =========================
    // Acciones principales
    // =========================
    private void cobrarReal() {
        try {
            if (lineas.isEmpty()) {
                Notification.show("Agrega al menos un plato a la venta.");
                return;
            }

            String ubic = ubicacionField.getValue();
            if (ubic == null || ubic.isBlank()) {
                ubicacionField.setInvalid(true);
                ubicacionField.setErrorMessage("Selecciona una mesa/pedido antes de cobrar.");
                Notification.show("Selecciona Mesa / Pedido.");
                return;
            }

            String forma = (formaPago.getValue() != null)
                    ? formaPago.getValue()
                    : "Efectivo";
            BigDecimal total = new BigDecimal(totalSpan.getText());

            if ("Efectivo".equals(forma)) {
                Double ef = efectivoField.getValue();
                if (ef == null) {
                    Notification.show("Ingresa el efectivo recibido.");
                    return;
                }
                if (BigDecimal.valueOf(ef).compareTo(total) < 0) {
                    Notification.show("El efectivo es menor que el total.");
                    return;
                }
            }

            // Tipo cliente y datos
            String tipoCliUI = tipoCliente.getValue();
            String tipoCli = "Con datos".equals(tipoCliUI) ? "CON_DATOS" : "FINAL";
            Cliente cliente = null;
            if ("CON_DATOS".equals(tipoCli)) {
                cliente = new Cliente();
                cliente.setNombre(nombreCliente.getValue());
                cliente.setNumeroIdentificacion(numeroDocumento.getValue());
                cliente.setCorreo(correoCliente.getValue());
                String tdoc = (tipoDocumento.getValue() != null)
                        ? tipoDocumento.getValue().toLowerCase()
                        : "";
                if (tdoc.contains("ruc")) {
                    cliente.setTipoIdentificacionSri("04");
                } else if (tdoc.contains("cédula") || tdoc.contains("cedula")) {
                    cliente.setTipoIdentificacionSri("05");
                } else {
                    cliente.setTipoIdentificacionSri("06");
                }
            }

            // Ítems desde la UI
            List<VentaService.Item> items = lineas.stream()
                    .map(l -> new VentaService.Item(
                            l.getProducto().getId(),
                            l.getCantidad(),
                            l.getPrecioUnitario(),
                            l.getNotaCocina()
                    ))
                    .toList();

            // Registrar venta completa
            Venta venta = ventaService.registrarVentaDesdeLineas(
                    items, forma, ubic, cliente, tipoCli);

            // Comanda cocina (ignora error)
            try {
                impresionCocinaService.imprimirComanda(venta);
            } catch (Exception ignored) {}

            // Cambio (solo efectivo)
            if ("Efectivo".equals(forma)) {
                BigDecimal efectivo = BigDecimal.valueOf(efectivoField.getValue());
                BigDecimal cambio = efectivo.subtract(venta.getTotal())
                        .setScale(2, RoundingMode.HALF_UP);
                cambioField.setValue(Math.max(0, cambio.doubleValue()));
            } else {
                cambioField.clear();
            }

            mostrarDialogoConfirmacionVenta(venta, ubic);

            // Reset UI
            lineas.clear();
            actualizarDetalle();
            ubicacionField.clear();
            tipoCliente.setValue("Consumidor final");
            tipoDocumento.clear();
            numeroDocumento.clear();
            nombreCliente.clear();
            correoCliente.clear();
            efectivoField.clear();

            actualizarEstadoDelDia();
            recalcularTotales();
            actualizarEstadoBotonCobrar();

        } catch (Exception ex) {
            Notification.show("Error al cobrar: " +
                    (ex.getMessage() != null ? ex.getMessage() : "Desconocido"));
            ex.printStackTrace();
        }
    }

    private void limpiarVenta() {
        lineas.clear();
        actualizarDetalle();
        ubicacionField.clear();
        efectivoField.clear();
        cambioField.clear();
        recalcularTotales();
        actualizarEstadoBotonCobrar();
    }

    private void mostrarDialogoConfirmacionVenta(Venta venta, String ubicacion) {
        Dialog dialog = new Dialog();

        H3 titulo = new H3("Venta registrada correctamente");

        Span linea1 = new Span("N° de venta: " + venta.getId());
        Span lineaMesa = new Span("Mesa / pedido: " + ubicacion);
        Span linea3 = new Span("Total: " +
                venta.getTotal().setScale(2, RoundingMode.HALF_UP));
        Span linea4 = new Span("Estado SRI: " +
                (venta.getEstadoSri() != null
                        ? venta.getEstadoSri()
                        : "PENDIENTE"));

        VerticalLayout contenido =
                new VerticalLayout(titulo, linea1, lineaMesa, linea3, linea4);
        contenido.setPadding(false);
        contenido.setSpacing(false);
        contenido.setAlignItems(FlexComponent.Alignment.START);

        Button ticketBtn = new Button("Imprimir ticket (orden)", e ->
                UI.getCurrent().getPage()
                        .open("ticket/" + venta.getId(), "_blank")
        );
        Button pdfBtn = new Button("Ver RIDE (PDF)", e ->
                UI.getCurrent().getPage()
                        .open("/api/ventas/" + venta.getId() + "/ride", "_blank")
        );
        Button cerrar = new Button("Cerrar", e -> dialog.close());
        cerrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout acciones =
                new HorizontalLayout(ticketBtn, pdfBtn, cerrar);
        acciones.setWidthFull();
        acciones.setJustifyContentMode(
                FlexComponent.JustifyContentMode.END);

        contenido.add(acciones);
        dialog.add(contenido);
        dialog.open();
    }

    // =========================
    // Utilidades UI
    // =========================
    private void agregarOIncrementarLinea(Producto p) {
        if (p == null) return;

        LineaUI existente = lineas.stream()
                .filter(l -> l.getProducto().getId().equals(p.getId()))
                .findFirst()
                .orElse(null);

        if (existente != null) {
            existente.setCantidad(existente.getCantidad() + 1);
        } else {
            BigDecimal precio = p.getPrecio() != null ? p.getPrecio() : BigDecimal.ZERO;
            lineas.add(new LineaUI(p, 1, precio, ""));
        }
    }

    private void actualizarDetalle() {
        detalleGrid.getDataProvider().refreshAll();
    }

    private void recalcularTotales() {
        BigDecimal total = lineas.stream()
                .map(LineaUI::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalSpan.setText(
                total.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    private void actualizarEstadoDelDia() {
        try {
            LocalDateTime ini = LocalDate.now().atStartOfDay();
            LocalDateTime fin = ini.plusDays(1);
            BigDecimal totalDia = ventaRepository
                    .findByFechaHoraBetween(ini, fin).stream()
                    .map(Venta::getTotal)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalDiaSpan.setText(
                    totalDia.setScale(2, RoundingMode.HALF_UP)
                            .toPlainString());
        } catch (Exception ex) {
            totalDiaSpan.setText("0.00");
        }
    }

    private void actualizarEstadoBotonCobrar() {
        boolean ok = !lineas.isEmpty()
                && ubicacionField.getValue() != null
                && !ubicacionField.getValue().isBlank()
                && (!"Efectivo".equals(formaPago.getValue())
                || (efectivoField.getValue() != null
                && efectivoField.getValue() >= 0));
        cobrarBtn.setEnabled(ok);

        if (ok) {
            cobrarBtn.addClassName("cobrar-activo");
        } else {
            cobrarBtn.removeClassName("cobrar-activo");
        }
    }

    /**
     * Helper central para no repetir:
     * refresca grid, totales y estado del botón Cobrar.
     */
    private void refrescarDespuesDeCambioDeLineas() {
        actualizarDetalle();
        recalcularTotales();
        actualizarEstadoBotonCobrar();
    }

    // =========================
    // Clase auxiliar para el Grid
    // =========================
    public static class LineaUI {
        private Producto producto;
        private int cantidad;
        private BigDecimal precioUnitario;
        private String notaCocina;

        public LineaUI(Producto producto,
                       int cantidad,
                       BigDecimal precioUnitario,
                       String notaCocina) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
            this.notaCocina = notaCocina;
        }

        public Producto getProducto() { return producto; }
        public void setProducto(Producto producto) { this.producto = producto; }

        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) {
            this.cantidad = Math.max(1, cantidad);
        }

        public BigDecimal getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(BigDecimal precioUnitario) {
            this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
        }

        public String getNotaCocina() { return notaCocina; }
        public void setNotaCocina(String notaCocina) { this.notaCocina = notaCocina; }

        public BigDecimal getSubtotal() {
            return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    // (Si quieres mostrar “Consumidor final / Con datos” en otros lugares)
    private String formatearTipoCliente(String tipo) {
        if ("CON_DATOS".equalsIgnoreCase(tipo)) return "Con datos";
        return "Consumidor final";
    }
}
