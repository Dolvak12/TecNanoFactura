package com.tecnano.factura.views.auth;

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("login")
@RouteAlias("") // ✅ root "/" abre LoginView SIEMPRE
@PageTitle("Iniciar sesión")
@AnonymousAllowed
@CssImport("./styles/shared-styles.css")
@CssImport("./styles/pos.css")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final String STORAGE_KEY = "tn-theme";
    private final LoginForm loginForm = new LoginForm();

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean logged =
                auth != null &&
                        auth.isAuthenticated() &&
                        !(auth instanceof AnonymousAuthenticationToken);

        if (logged) {
            event.forwardTo("home"); // ✅ HomeView ahora será /home
            return;
        }

        // Mostrar error cuando Spring Security redirige a /login?error
        boolean hasError = event.getLocation().getQueryParameters().getParameters().containsKey("error");
        loginForm.setError(hasError);
    }

    public LoginView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        // Para que use el fondo del tema global
        getStyle().set("background", "transparent");

        // ✅ Aplicar tema guardado también en Login
        addAttachListener(e ->
                UI.getCurrent().getPage()
                        .executeJs("return localStorage.getItem($0)", STORAGE_KEY)
                        .then(String.class, value -> {
                            boolean wantDark = (value == null || value.isBlank()) || "dark".equalsIgnoreCase(value);
                            if (wantDark) UI.getCurrent().getElement().getThemeList().add(Lumo.DARK);
                            else UI.getCurrent().getElement().getThemeList().remove(Lumo.DARK);
                        })
        );

        // ---------- Header ----------
        H1 tituloApp = new H1("TecNanoFactura");
        tituloApp.getStyle()
                .set("margin-top", "2rem")
                .set("margin-bottom", "0.25rem");

        Span subtitulo = new Span("POS para restaurantes");
        subtitulo.getStyle()
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(tituloApp, subtitulo);
        header.setPadding(false);
        header.setSpacing(false);
        header.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        // ---------- Card ----------
        Div card = new Div();
        card.getStyle()
                .set("max-width", "420px")
                .set("width", "100%")
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        VerticalLayout cardContent = new VerticalLayout();
        cardContent.setPadding(true);
        cardContent.setSpacing(true);
        cardContent.setWidthFull();
        cardContent.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);

        H1 tituloLogin = new H1("Iniciar sesión");
        tituloLogin.getStyle()
                .set("font-size", "1.3rem")
                .set("margin", "0");

        Span ayuda = new Span("Usuario: admin / caja   ·   Clave: 1234");
        ayuda.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "var(--lumo-secondary-text-color)");

        loginForm.setAction("login"); // endpoint de Spring Security
        loginForm.setI18n(createSpanishI18n());
        loginForm.setForgotPasswordButtonVisible(false);

        cardContent.add(tituloLogin, ayuda, new HtmlComponent("hr"), loginForm);
        card.add(cardContent);

        add(header, card);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    }

    private LoginI18n createSpanishI18n() {
        LoginI18n i18n = LoginI18n.createDefault();

        LoginI18n.Form form = i18n.getForm();
        form.setTitle("Iniciar sesión");
        form.setUsername("Usuario");
        form.setPassword("Contraseña");
        form.setSubmit("Entrar");
        form.setForgotPassword("¿Olvidaste tu contraseña?");
        i18n.setForm(form);

        LoginI18n.ErrorMessage error = i18n.getErrorMessage();
        error.setTitle("Datos incorrectos");
        error.setMessage("Revisa tu usuario y contraseña e inténtalo de nuevo.");
        i18n.setErrorMessage(error);

        LoginI18n.Header header = new LoginI18n.Header();
        header.setTitle("TecNanoFactura");
        header.setDescription("POS para restaurantes");
        i18n.setHeader(header);

        i18n.setAdditionalInformation(
                "Acceso restringido. Si tienes dudas, consulta con el administrador del sistema."
        );

        return i18n;
    }
}
