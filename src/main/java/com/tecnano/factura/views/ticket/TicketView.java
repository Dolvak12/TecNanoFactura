package com.tecnano.factura.views.ticket;

import com.tecnano.factura.config.AppProperties;
import com.tecnano.factura.model.DetalleVenta;
import com.tecnano.factura.model.Venta;
import com.tecnano.factura.repository.VentaRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.Lumo;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Route("ticket")
@PageTitle("Ticket")
@PermitAll
public class TicketView extends VerticalLayout implements HasUrlParameter<Long> {

    private static final String STORAGE_KEY = "tn-theme";

    private final VentaRepository ventaRepository;
    private final AppProperties appProperties;

    private final DateTimeFormatter fechaHoraFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TicketView(VentaRepository ventaRepository,
                      AppProperties appProperties) {
        this.ventaRepository = ventaRepository;
        this.appProperties = appProperties;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // Fondo bonito según tema
        getStyle().set("background-color", "transparent");

        addAttachListener(e -> applyThemeFromStorage());
    }

    private void applyThemeFromStorage() {
        UI.getCurrent().getPage()
                .executeJs("return localStorage.getItem($0)", STORAGE_KEY)
                .then(String.class, value -> {
                    boolean wantDark = (value == null || value.isBlank()) || "dark".equalsIgnoreCase(value);
                    if (wantDark) UI.getCurrent().getElement().getThemeList().add(Lumo.DARK);
                    else UI.getCurrent().getElement().getThemeList().remove(Lumo.DARK);
                });
    }

    private void doLogout() {
        UI.getCurrent().getPage().executeJs("""
            const getCookie = (name) => {
              const m = document.cookie.match(new RegExp('(?:^|; )' + name.replace(/([.$?*|{}()\\[\\]\\\\\\/\\+^])/g, '\\\\$1') + '=([^;]*)'));
              return m ? decodeURIComponent(m[1]) : '';
            };
            const token = getCookie('XSRF-TOKEN');
            fetch($0, { method: 'POST', headers: token ? {'X-XSRF-TOKEN': token} : {} })
              .then(r => window.location.href = $1)
              .catch(() => window.location.href = $2);
        """, "/logout", "/login", "/logout");
    }

    @Override
    public void setParameter(BeforeEvent event,
                             @OptionalParameter Long id) {
        removeAll();

        // Barra superior (no se imprime)
        Button volverBtn = new Button("Volver", VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().navigate(""));
        volverBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button imprimirTop = new Button("Imprimir", VaadinIcon.PRINT.create(), e ->
                UI.getCurrent().getPage().executeJs("window.print();")
        );
        imprimirTop.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button salirBtn = new Button("Salir", VaadinIcon.SIGN_OUT.create(), e -> doLogout());
        salirBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout top = new HorizontalLayout(volverBtn, new Span(), imprimirTop, salirBtn);
        top.setWidthFull();
        top.expand(top.getComponentAt(1));
        top.setAlignItems(Alignment.CENTER);
        top.addClassName("tn-no-print"); // se oculta al imprimir (lo añadimos en CSS)
        add(top);

        if (id == null) {
            add(new Span("No se indicó el número de venta."));
            return;
        }

        Optional<Venta> opt = ventaRepository.findById(id);
        if (opt.isEmpty()) {
            add(new Span("Venta no encontrada: " + id));
            return;
        }

        construirTicket(opt.get());
    }

    private void construirTicket(Venta venta) {
        VerticalLayout papel = new VerticalLayout();
        papel.setWidth("340px");
        papel.getStyle()
                .set("background-color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 0 10px rgba(0,0,0,0.10)")
                .set("padding", "0.75rem")
                .set("font-family", "monospace")
                .set("font-size", "0.8rem");
        papel.setSpacing(false);
        papel.setPadding(false);
        papel.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);

        AppProperties.Negocio neg = appProperties.getNegocio();

        H2 nombreNegocio = new H2(neg.getNombre());
        nombreNegocio.getStyle()
                .set("font-size", "1.1rem")
                .set("margin", "0")
                .set("text-align", "center");

        Span ruc = new Span("RUC: " + safe(neg.getRuc()));
        Span regimen = new Span(safe(neg.getRegimen()));
        Span dir = new Span("Dir: " + safe(neg.getDireccion()));
        Span tel = new Span("Tel: " + safe(neg.getTelefono()));

        ruc.getStyle().set("font-size", "0.75rem");
        regimen.getStyle().set("font-size", "0.75rem");
        dir.getStyle().set("font-size", "0.75rem");
        tel.getStyle().set("font-size", "0.75rem");

        VerticalLayout headerNegocio = new VerticalLayout(nombreNegocio, ruc, regimen, dir, tel);
        headerNegocio.setPadding(false);
        headerNegocio.setSpacing(false);
        headerNegocio.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        headerNegocio.getStyle().set("margin-bottom", "0.5rem");

        papel.add(headerNegocio, new Hr());

        String fechaHora = venta.getFechaHora() != null ? fechaHoraFormatter.format(venta.getFechaHora()) : "--";
        String ubic = venta.getUbicacion() != null ? venta.getUbicacion() : venta.getMesa();

        Span sNum = new Span("Venta N°: " + venta.getId());
        Span sFecha = new Span("Fecha: " + fechaHora);
        Span sMesa = new Span("Mesa/Pedido: " + (ubic != null ? ubic : "-"));
        Span sPago = new Span("Pago: " + safe(venta.getFormaPago()));

        sNum.getStyle().set("font-size", "0.8rem");
        sFecha.getStyle().set("font-size", "0.8rem");
        sMesa.getStyle().set("font-size", "0.8rem");
        sPago.getStyle().set("font-size", "0.8rem");

        VerticalLayout datosVentaLayout = new VerticalLayout(sNum, sFecha, sMesa, sPago);
        datosVentaLayout.setPadding(false);
        datosVentaLayout.setSpacing(false);
        datosVentaLayout.getStyle().set("margin-bottom", "0.35rem");

        papel.add(datosVentaLayout, new Hr());

        if (venta.getCliente() != null) {
            var c = venta.getCliente();
            H3 tituloCli = new H3("Cliente");
            tituloCli.getStyle().set("margin", "0").set("font-size", "0.85rem");

            Span cliNombre = new Span("Nombre: " + safe(c.getNombre()));
            Span cliDoc = new Span("Doc: " + safe(c.getNumeroIdentificacion()));
            Span cliCorreo = new Span("Correo: " + safe(c.getCorreo()));

            cliNombre.getStyle().set("font-size", "0.75rem");
            cliDoc.getStyle().set("font-size", "0.75rem");
            cliCorreo.getStyle().set("font-size", "0.75rem");

            VerticalLayout cliLayout = new VerticalLayout(tituloCli, cliNombre, cliDoc, cliCorreo);
            cliLayout.setPadding(false);
            cliLayout.setSpacing(false);
            cliLayout.getStyle().set("margin-bottom", "0.35rem");

            papel.add(cliLayout, new Hr());
        }

        H3 tituloDet = new H3("Detalle");
        tituloDet.getStyle().set("margin", "0").set("font-size", "0.85rem");
        papel.add(tituloDet);

        HorizontalLayout cabecera = new HorizontalLayout();
        cabecera.setWidthFull();
        cabecera.setAlignItems(Alignment.CENTER);
        cabecera.setSpacing(false);

        Span cCant = new Span("Cant");
        Span cDesc = new Span("Descripción");
        Span cSub = new Span("Sub");

        cCant.getStyle().set("width", "40px");
        cDesc.getStyle().set("flex-grow", "1");
        cSub.getStyle().set("width", "70px");

        cCant.getStyle().set("font-weight", "600").set("font-size", "0.75rem");
        cDesc.getStyle().set("font-weight", "600").set("font-size", "0.75rem");
        cSub.getStyle().set("font-weight", "600").set("font-size", "0.75rem");

        cabecera.add(cCant, cDesc, cSub);
        papel.add(cabecera, new Hr());

        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (DetalleVenta d : venta.getDetalles()) {
                if (d == null || d.getProducto() == null) continue;

                int cant = d.getCantidad();
                String nombre = Objects.toString(d.getProducto().getNombre(), "(sin nombre)");
                BigDecimal precioUnit = d.getPrecioUnitario() != null ? d.getPrecioUnitario() : BigDecimal.ZERO;
                BigDecimal subtotal = precioUnit.multiply(BigDecimal.valueOf(cant));

                HorizontalLayout fila = new HorizontalLayout();
                fila.setWidthFull();
                fila.setSpacing(false);
                fila.setAlignItems(Alignment.START);

                Span sCant = new Span(String.valueOf(cant));
                sCant.getStyle().set("width", "40px").set("font-size", "0.75rem");

                Span sDesc = new Span(nombre);
                sDesc.getStyle().set("flex-grow", "1").set("font-size", "0.75rem");

                Span sSub = new Span("$" + subtotal.setScale(2, RoundingMode.HALF_UP));
                sSub.getStyle().set("width", "70px").set("text-align", "right").set("font-size", "0.75rem");

                fila.add(sCant, sDesc, sSub);
                papel.add(fila);

                if (d.getNotaCocina() != null && !d.getNotaCocina().isBlank()) {
                    Span sNota = new Span("  Nota: " + d.getNotaCocina());
                    sNota.getStyle().set("font-size", "0.7rem").set("font-style", "italic");
                    VerticalLayout notaWrapper = new VerticalLayout(sNota);
                    notaWrapper.setPadding(false);
                    notaWrapper.setSpacing(false);
                    notaWrapper.getStyle().set("margin-left", "40px");
                    papel.add(notaWrapper);
                }
            }
        } else {
            Span vacio = new Span("(Sin detalle de productos)");
            vacio.getStyle().set("font-size", "0.75rem");
            papel.add(vacio);
        }

        papel.add(new Hr());

        BigDecimal subtotal = venta.getSubtotal() != null ? venta.getSubtotal() : BigDecimal.ZERO;
        BigDecimal iva = venta.getIva() != null ? venta.getIva() : BigDecimal.ZERO;
        BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;

        papel.add(crearLineaTotal("Subtotal:", subtotal));
        papel.add(crearLineaTotal("IVA:", iva));
        papel.add(crearLineaTotal("TOTAL:", total, true));

        papel.add(new Hr());

        Span gracias = new Span("¡Gracias por su compra!");
        gracias.getStyle()
                .set("display", "block")
                .set("text-align", "center")
                .set("margin-top", "0.25rem")
                .set("font-size", "0.8rem");

        papel.add(gracias);

        Button imprimirBtn = new Button("Imprimir ticket", e ->
                UI.getCurrent().getPage().executeJs("window.print();")
        );
        imprimirBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        imprimirBtn.addClassName("tn-no-print");
        imprimirBtn.getStyle().set("margin-top", "0.75rem");

        VerticalLayout wrapper = new VerticalLayout(papel, imprimirBtn);
        wrapper.setPadding(false);
        wrapper.setSpacing(true);
        wrapper.setAlignItems(Alignment.CENTER);

        add(wrapper);
    }

    private HorizontalLayout crearLineaTotal(String label, BigDecimal valor) {
        return crearLineaTotal(label, valor, false);
    }

    private HorizontalLayout crearLineaTotal(String label, BigDecimal valor, boolean resaltar) {
        HorizontalLayout hl = new HorizontalLayout();
        hl.setWidthFull();
        hl.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        hl.setAlignItems(Alignment.CENTER);
        hl.setSpacing(false);

        Span sLabel = new Span(label);
        Span sVal = new Span("$" + valor.setScale(2, RoundingMode.HALF_UP));

        sLabel.getStyle().set("font-size", "0.8rem");
        sVal.getStyle().set("font-size", "0.8rem");

        if (resaltar) {
            sLabel.getStyle().set("font-weight", "700");
            sVal.getStyle().set("font-weight", "700");
        }

        hl.add(sLabel, sVal);
        return hl;
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
