package com.tecnano.factura.views;

import com.tecnano.factura.security.SecurityService;
import com.tecnano.factura.views.admin.AdminToolsView;
import com.tecnano.factura.views.caja.CajaView;
import com.tecnano.factura.views.cierre.CierreCajaView;
import com.tecnano.factura.views.cocina.CocinaView;
import com.tecnano.factura.views.home.HomeView;
import com.tecnano.factura.views.productos.ProductoCrudView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private static final String STORAGE_KEY = "tn-theme";

    private final SecurityService securityService;

    private final Button backBtn = new Button(VaadinIcon.ARROW_LEFT.create());
    private final Button themeBtn = new Button();
    private final Button logoutBtn = new Button("Salir", VaadinIcon.SIGN_OUT.create());

    private boolean dark = true; // default oscuro

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;

        addClassName("tn-app");
        setPrimarySection(Section.DRAWER);

        DrawerToggle toggle = new DrawerToggle();

        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        backBtn.addClassName("tn-back-btn");
        backBtn.getElement().setProperty("title", "Atrás");
        backBtn.addClickListener(e -> UI.getCurrent().getPage().getHistory().back());

        Span title = new Span("TecNanoFactura");
        title.addClassName("tn-toolbar-title");

        themeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        themeBtn.addClassName("tn-theme-btn");
        themeBtn.addClickListener(e -> toggleTheme());

        logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        logoutBtn.addClassName("tn-logout-btn");
        logoutBtn.getElement().setProperty("title", "Cerrar sesión");
        logoutBtn.addClickListener(e -> this.securityService.logout());

        HorizontalLayout actions = new HorizontalLayout(themeBtn, logoutBtn);
        actions.setSpacing(false);
        actions.addClassName("tn-toolbar-actions");

        HorizontalLayout navbar = new HorizontalLayout(toggle, backBtn, title, new Div(), actions);
        navbar.setWidthFull();
        navbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        navbar.expand(navbar.getComponentAt(3));
        navbar.addClassName("tn-navbar");

        addToNavbar(navbar);

        addToDrawer(buildDrawer());

        addAttachListener(e -> applyThemeFromStorage());
        updateThemeButton();
    }

    private Div buildDrawer() {
        Div header = new Div();
        header.addClassName("tn-drawer-header");

        // Logo (asegúrate que exista en /img/logo-tecnano.png)
        Image logo = new Image("/img/logo-tecnano.png", "TecNanoFactura");
        logo.addClassName("tn-logo");
        logo.setHeight("44px");

        Div brand = new Div();
        brand.addClassName("tn-brand");

        H2 appName = new H2("Menú");
        appName.getStyle().set("margin", "0");
        Span sub = new Span("POS / Cocina / Cierre");
        sub.addClassName("tn-drawer-sub");

        Div texts = new Div(appName, sub);
        texts.addClassName("tn-brand-texts");
        brand.add(logo, texts);

        header.add(brand);

        SideNav nav = new SideNav();
        nav.addClassName("tn-sidenav");

        nav.addItem(new SideNavItem("Inicio", HomeView.class, VaadinIcon.HOME.create()));
        nav.addItem(new SideNavItem("Caja", CajaView.class, VaadinIcon.CASH.create()));
        nav.addItem(new SideNavItem("Cocina", CocinaView.class, VaadinIcon.COFFEE.create()));
        nav.addItem(new SideNavItem("Cierre de caja", CierreCajaView.class, VaadinIcon.CALENDAR_CLOCK.create()));

        if (isAdmin()) {
            nav.addItem(new SideNavItem("Productos", ProductoCrudView.class, VaadinIcon.CLIPBOARD_TEXT.create()));
            nav.addItem(new SideNavItem("Admin tools", AdminToolsView.class, VaadinIcon.TOOLS.create()));
        }

        Div footer = new Div();
        footer.addClassName("tn-drawer-footer");
        footer.add(new Span("© TecNano"));

        Div wrapper = new Div(header, nav, footer);
        wrapper.addClassName("tn-drawer");
        return wrapper;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String path = event.getLocation().getPath();
        boolean onHome = (path == null || path.isBlank() || "home".equals(path));
        backBtn.setVisible(!onHome);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private void applyThemeFromStorage() {
        UI ui = UI.getCurrent();
        ui.getPage()
                .executeJs("return localStorage.getItem($0)", STORAGE_KEY)
                .then(String.class, value -> {
                    boolean wantDark = (value == null || value.isBlank()) || "dark".equalsIgnoreCase(value);
                    setDark(wantDark);
                });
    }

    private void toggleTheme() {
        setDark(!dark);
    }

    private void setDark(boolean value) {
        this.dark = value;

        if (dark) UI.getCurrent().getElement().getThemeList().add(Lumo.DARK);
        else UI.getCurrent().getElement().getThemeList().remove(Lumo.DARK);

        UI.getCurrent().getPage().executeJs(
                "localStorage.setItem($0, $1)",
                STORAGE_KEY,
                dark ? "dark" : "light"
        );

        updateThemeButton();
    }

    private void updateThemeButton() {
        themeBtn.setText("");
        themeBtn.setIcon(dark ? VaadinIcon.SUN_O.create() : VaadinIcon.MOON_O.create());
        themeBtn.getElement().setProperty("title", dark ? "Cambiar a modo claro" : "Cambiar a modo oscuro");
    }
}
