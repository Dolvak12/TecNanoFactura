package com.tecnano.factura.views.productos;

import com.tecnano.factura.model.Producto;
import com.tecnano.factura.repository.ProductoRepository;
import com.tecnano.factura.ui.Notificaciones;
import com.tecnano.factura.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Route(value = "productos", layout = MainLayout.class)
@PageTitle("Productos")
@RolesAllowed("ADMIN")
@CssImport("./styles/shared-styles.css")
@CssImport("./styles/pos.css")
public class ProductoCrudView extends VerticalLayout {

    private final ProductoRepository productoRepository;

    private final Grid<Producto> grid = new Grid<>(Producto.class, false);

    private final TextField filtro = new TextField("Buscar");
    private final TextField codigoField = new TextField("Código");
    private final TextField nombreField = new TextField("Nombre");
    private final NumberField precioField = new NumberField("Precio (USD)");
    private final NumberField stockField = new NumberField("Stock (opcional)");
    private final Checkbox conIvaField = new Checkbox("Aplica IVA (12%)");

    private final Button nuevoBtn = new Button("Nuevo");
    private final Button guardarBtn = new Button("Guardar");
    private final Button eliminarBtn = new Button("Eliminar");
    private final Button cancelarBtn = new Button("Cancelar");

    private Producto productoActual;

    public ProductoCrudView(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H1 titulo = new H1("Catálogo de productos");
        titulo.getStyle().set("margin-bottom", "0.25rem");

        Span ayuda = new Span("Aquí se configuran los platos y bebidas que aparecen en la Caja.");
        ayuda.getStyle()
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-secondary-text-color)");

        filtro.setPlaceholder("Código o nombre...");
        filtro.setClearButtonVisible(true);
        filtro.setWidth("300px");
        filtro.addValueChangeListener(e -> refrescarGrid());

        HorizontalLayout filtroLayout = new HorizontalLayout(filtro);
        filtroLayout.setWidthFull();
        filtroLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        Div cardGrid = new Div(crearSeccionGrid());
        cardGrid.addClassName("card-blanca");
        cardGrid.setWidthFull();

        Div cardForm = new Div(crearSeccionFormulario());
        cardForm.addClassName("card-blanca");
        cardForm.setWidth("380px");

        HorizontalLayout contenido = new HorizontalLayout(cardGrid, cardForm);
        contenido.setWidthFull();
        contenido.setSpacing(true);
        contenido.setAlignItems(FlexComponent.Alignment.START);
        contenido.setFlexGrow(1, cardGrid);

        add(titulo, ayuda, filtroLayout, contenido);

        configurarEventos();
        refrescarGrid();
        limpiarFormulario();
    }

    // =========================
    // Sección GRID
    // =========================

    private VerticalLayout crearSeccionGrid() {
        configurarGrid();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();

        layout.add(grid);
        return layout;
    }

    private void configurarGrid() {
        grid.addColumn(Producto::getCodigo)
                .setHeader("Código")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(Producto::getNombre)
                .setHeader("Nombre")
                .setFlexGrow(1)
                .setSortable(true);

        grid.addColumn(p -> {
                    BigDecimal precio = Optional.ofNullable(p.getPrecio())
                            .orElse(BigDecimal.ZERO);
                    return precio.setScale(2, RoundingMode.HALF_UP).toPlainString();
                })
                .setHeader("Precio")
                .setAutoWidth(true);

        grid.addColumn(p -> {
                    Integer stock = p.getStock();
                    return stock != null ? stock : 0;
                })
                .setHeader("Stock")
                .setAutoWidth(true);

        grid.addColumn(p -> p.isConIva() ? "Sí" : "No")
                .setHeader("IVA")
                .setAutoWidth(true);

        grid.setHeight("500px");
        grid.setWidthFull();

        grid.asSingleSelect().addValueChangeListener(e -> {
            Producto seleccionado = e.getValue();
            if (seleccionado != null) {
                editarProducto(seleccionado);
            }
        });
    }

    // =========================
    // Sección FORMULARIO
    // =========================

    private VerticalLayout crearSeccionFormulario() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();

        Span tituloForm = new Span("Editar / crear producto");
        tituloForm.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.95rem");

        codigoField.setWidthFull();
        nombreField.setWidthFull();

        precioField.setWidthFull();
        precioField.setStep(0.01);
        precioField.setMin(0);

        stockField.setWidthFull();
        stockField.setStep(1);
        stockField.setMin(0);

        conIvaField.getStyle().set("margin-top", "0.5rem");

        nuevoBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        guardarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        eliminarBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        cancelarBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout botones = new HorizontalLayout(nuevoBtn, guardarBtn, eliminarBtn, cancelarBtn);
        botones.setWidthFull();
        botones.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        layout.add(
                tituloForm,
                codigoField,
                nombreField,
                precioField,
                stockField,
                conIvaField,
                botones
        );

        return layout;
    }

    // =========================
    // Eventos
    // =========================

    private void configurarEventos() {
        nuevoBtn.addClickListener(e -> limpiarFormulario());
        guardarBtn.addClickListener(e -> guardarProducto());
        eliminarBtn.addClickListener(e -> eliminarProducto());
        cancelarBtn.addClickListener(e -> {
            if (productoActual != null) {
                editarProducto(productoActual);
            } else {
                limpiarFormulario();
            }
        });
    }

    // =========================
    // Lógica CRUD
    // =========================

    private void refrescarGrid() {
        List<Producto> productos = productoRepository.findAll();

        String term = filtro.getValue();
        if (term != null && !term.isBlank()) {
            String t = term.toLowerCase();
            productos = productos.stream()
                    .filter(p ->
                            (p.getNombre() != null && p.getNombre().toLowerCase().contains(t)) ||
                                    (p.getCodigo() != null && p.getCodigo().toLowerCase().contains(t))
                    )
                    .toList();
        }

        grid.setItems(productos);
    }

    private void editarProducto(Producto producto) {
        this.productoActual = producto;

        codigoField.setValue(producto.getCodigo() != null ? producto.getCodigo() : "");
        nombreField.setValue(producto.getNombre() != null ? producto.getNombre() : "");

        BigDecimal precio = producto.getPrecio() != null ? producto.getPrecio() : BigDecimal.ZERO;
        precioField.setValue(precio.setScale(2, RoundingMode.HALF_UP).doubleValue());

        if (producto.getStock() != null) {
            stockField.setValue(producto.getStock().doubleValue());
        } else {
            stockField.clear();
        }

        conIvaField.setValue(producto.isConIva());
    }

    private void limpiarFormulario() {
        this.productoActual = null;
        grid.asSingleSelect().clear();

        codigoField.clear();
        nombreField.clear();
        precioField.clear();
        stockField.clear();
        conIvaField.setValue(false);
    }

    private void guardarProducto() {
        String codigo = codigoField.getValue();
        String nombre = nombreField.getValue();
        Double precioDouble = precioField.getValue();

        if (codigo == null || codigo.isBlank()) {
            Notificaciones.advertencia("Ingresa el código del producto.");
            return;
        }
        if (nombre == null || nombre.isBlank()) {
            Notificaciones.advertencia("Ingresa el nombre del producto.");
            return;
        }
        if (precioDouble == null || precioDouble < 0) {
            Notificaciones.advertencia("Ingresa un precio válido.");
            return;
        }

        if (productoActual == null) {
            productoActual = new Producto();
        }

        productoActual.setCodigo(codigo.trim());
        productoActual.setNombre(nombre.trim());

        BigDecimal precio = BigDecimal.valueOf(precioDouble)
                .setScale(2, RoundingMode.HALF_UP);
        productoActual.setPrecio(precio);

        Double stockDouble = stockField.getValue();
        if (stockDouble == null) {
            productoActual.setStock(null);
        } else {
            int stock = (int) Math.max(0, Math.floor(stockDouble));
            productoActual.setStock(stock);
        }

        productoActual.setConIva(Boolean.TRUE.equals(conIvaField.getValue()));

        try {
            productoRepository.save(productoActual);
            Notificaciones.exito("Producto guardado correctamente.");
            refrescarGrid();
            limpiarFormulario();
        } catch (Exception ex) {
            Notificaciones.error("Error al guardar producto: " +
                    (ex.getMessage() != null ? ex.getMessage() : "desconocido"));
        }
    }

    private void eliminarProducto() {
        if (productoActual == null || productoActual.getId() == null) {
            Notificaciones.advertencia("Selecciona un producto para eliminar.");
            return;
        }

        try {
            productoRepository.delete(productoActual);
            Notificaciones.exito("Producto eliminado.");
            refrescarGrid();
            limpiarFormulario();
        } catch (Exception ex) {
            Notificaciones.error("Error al eliminar producto: " +
                    (ex.getMessage() != null ? ex.getMessage() : "desconocido"));
        }
    }
}
