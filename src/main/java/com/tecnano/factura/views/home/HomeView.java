package com.tecnano.factura.views.home;

import com.tecnano.factura.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "home", layout = MainLayout.class)
@PageTitle("Inicio | TecNanoFactura")
@RolesAllowed({"USER", "ADMIN"})
@CssImport("./styles/shared-styles.css")
@CssImport("./styles/pos.css")
public class HomeView extends VerticalLayout {

    public HomeView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        addClassName("home-view");

        H1 titulo = new H1("TecNanoFactura");
        titulo.getStyle()
                .set("margin-top", "0.5rem")
                .set("margin-bottom", "0.25rem");

        Span subtitulo = new Span("¿Qué quieres hacer?");
        subtitulo.getStyle()
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(titulo, subtitulo);
        header.setPadding(false);
        header.setSpacing(false);
        header.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        header.addClassName("home-header");

        Div grid = new Div();
        grid.addClassName("main-menu-grid");

        // Siempre visibles
        grid.add(
                crearCardMenu("Caja", "Registrar ventas y cobrar rápido", VaadinIcon.CASH, "caja"),
                crearCardMenu("Cocina", "Ver las órdenes del día", VaadinIcon.COFFEE, "cocina"),
                crearCardMenu("Cierre de caja", "Ver totales y diferencias", VaadinIcon.CALENDAR_CLOCK, "cierre-caja")
        );

        // Solo ADMIN
        if (isAdmin()) {
            grid.add(
                    crearCardMenu("Productos", "Configurar platos y bebidas", VaadinIcon.CLIPBOARD_TEXT, "productos"),
                    crearCardMenu("Admin tools", "Backups y configuración avanzada", VaadinIcon.TOOLS, "admin-tools")
            );
        }

        add(header, grid);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Div crearCardMenu(String titulo,
                              String descripcion,
                              VaadinIcon icono,
                              String rutaNavegacion) {

        Div card = new Div();
        card.addClassName("card-blanca");
        card.addClassName("main-menu-card");

        Span iconWrapper = new Span(icono.create());
        iconWrapper.addClassName("main-menu-card-icon");

        Span titleSpan = new Span(titulo);
        titleSpan.addClassName("main-menu-card-title");

        Span descSpan = new Span(descripcion);
        descSpan.addClassName("main-menu-card-desc");

        Button irBtn = new Button("Abrir " + titulo, e -> UI.getCurrent().navigate(rutaNavegacion));
        irBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        irBtn.addClassName("main-menu-card-button");

        VerticalLayout content = new VerticalLayout(iconWrapper, titleSpan, descSpan, irBtn);
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(FlexComponent.Alignment.START);
        content.getStyle().set("gap", "0.4rem");

        card.add(content);

        // Click en toda la card
        card.getElement().addEventListener("click", e -> UI.getCurrent().navigate(rutaNavegacion));

        return card;
    }
}
