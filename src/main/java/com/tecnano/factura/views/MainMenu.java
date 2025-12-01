package com.tecnano.factura.views;

import com.tecnano.factura.views.admin.AdminToolsView;
import com.tecnano.factura.views.caja.CajaView;
import com.tecnano.factura.views.cierre.CierreCajaView;
import com.tecnano.factura.views.cocina.CocinaView;
import com.tecnano.factura.views.productos.ProductoCrudView;
import com.tecnano.factura.views.ventas.VentasPendientesSriView;
import com.tecnano.factura.views.ventas.VentasView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

public class MainMenu extends VerticalLayout {

    public MainMenu() {
        addClassName("main-menu");
        setPadding(true);
        setSpacing(false);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.STRETCH);

        Span title = new Span("Menú principal");
        title.addClassName("main-menu-title");
        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.9rem")
                .set("margin-bottom", "0.5rem");

        add(
                title,
                createNavLink("Caja", CajaView.class, VaadinIcon.CASH),
                createNavLink("Cocina", CocinaView.class, VaadinIcon.COFFEE),
                // Corregido: CLIPBOARD_LIST -> CLIPBOARD_TEXT (u otro icono válido que prefieras)
                createNavLink("Productos", ProductoCrudView.class, VaadinIcon.CLIPBOARD_TEXT),
                createNavLink("Ventas", VentasView.class, VaadinIcon.TRENDING_UP),
                createNavLink("Ventas pendientes SRI", VentasPendientesSriView.class, VaadinIcon.WARNING),
                createNavLink("Cierre de caja", CierreCajaView.class, VaadinIcon.ARCHIVE),
                createNavLink("Admin tools", AdminToolsView.class, VaadinIcon.COG)
        );
    }

    private RouterLink createNavLink(String text,
                                     Class<? extends Component> navigationTarget,
                                     VaadinIcon icon) {
        RouterLink link = new RouterLink();
        link.addClassName("main-menu-link");
        link.setRoute(navigationTarget);

        var iconCmp = icon.create();
        iconCmp.getStyle()
                .set("margin-right", "0.4rem")
                .set("font-size", "1.1rem");

        Span label = new Span(text);
        label.getStyle().set("font-size", "0.9rem");

        HorizontalLayout content = new HorizontalLayout(iconCmp, label);
        content.setSpacing(true);
        content.setAlignItems(FlexComponent.Alignment.CENTER);

        link.add(content);
        return link;
    }
}
