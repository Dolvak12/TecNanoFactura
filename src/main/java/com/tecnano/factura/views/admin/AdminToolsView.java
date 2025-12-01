// =====================================================
// src/main/java/com/tecnano/factura/views/admin/AdminToolsView.java
// =====================================================
package com.tecnano.factura.views.admin;

import com.tecnano.factura.config.AppProperties;
import com.tecnano.factura.service.BackupService;
import com.tecnano.factura.ui.Notificaciones;
import com.tecnano.factura.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

@Route(value = "admin-tools", layout = MainLayout.class)
@PageTitle("Admin tools")
@RolesAllowed("ADMIN")
@CssImport("./styles/shared-styles.css")
@CssImport("./styles/pos.css")
public class AdminToolsView extends VerticalLayout {

    private final AppProperties appProperties;
    private final BackupService backupService;

    private final TextField nombreNegocio = new TextField("Nombre del negocio");
    private final TextField rucNegocio = new TextField("RUC");
    private final TextField regimenNegocio = new TextField("Régimen");
    private final TextField dirNegocio = new TextField("Dirección");
    private final TextField telNegocio = new TextField("Teléfono");

    private final Checkbox sriSimular = new Checkbox("Simular envío a SRI (modo pruebas)");
    private final TextField sriBaseUrl = new TextField("Proveedor base URL");
    private final PasswordField sriToken = new PasswordField("Proveedor token/API key");

    private final TextField usbPathField = new TextField("Ruta USB (opcional)");

    public AdminToolsView(AppProperties appProperties,
                          BackupService backupService) {
        this.appProperties = appProperties;
        this.backupService = backupService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H1 titulo = new H1("Herramientas de administrador");
        titulo.getStyle().set("margin-bottom", "0.5rem");

        Span subtitulo = new Span("Configuración avanzada, SRI y backups.");
        subtitulo.getStyle()
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(titulo, subtitulo);
        header.setPadding(false);
        header.setSpacing(false);

        cargarValoresNegocio();
        cargarValoresSri();

        VerticalLayout cardNegocio = crearCardNegocio();
        VerticalLayout cardSri = crearCardSri();
        VerticalLayout cardBackup = crearCardBackup();

        HorizontalLayout fila1 = new HorizontalLayout(cardNegocio, cardSri);
        fila1.setWidthFull();
        fila1.setSpacing(true);
        fila1.setAlignItems(Alignment.START);
        fila1.setFlexGrow(1, cardNegocio, cardSri);

        add(header, fila1, cardBackup);
    }

    private VerticalLayout crearCardNegocio() {
        VerticalLayout layout = new VerticalLayout();
        layout.addClassName("card-blanca");
        layout.setWidth("100%");
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 titulo = new H3("Datos del negocio");
        titulo.getStyle().set("margin-top", "0").set("margin-bottom", "0.5rem");

        nombreNegocio.setWidthFull();
        rucNegocio.setWidthFull();
        regimenNegocio.setWidthFull();
        dirNegocio.setWidthFull();
        telNegocio.setWidthFull();

        Button guardar = new Button("Guardar (solo en memoria)", e -> guardarValoresNegocio());
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout acciones = new HorizontalLayout(guardar);
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        acciones.setWidthFull();

        layout.add(titulo,
                nombreNegocio,
                rucNegocio,
                regimenNegocio,
                dirNegocio,
                telNegocio,
                acciones);

        return layout;
    }

    private void cargarValoresNegocio() {
        AppProperties.Negocio n = appProperties.getNegocio();
        nombreNegocio.setValue(n.getNombre() != null ? n.getNombre() : "");
        rucNegocio.setValue(n.getRuc() != null ? n.getRuc() : "");
        regimenNegocio.setValue(n.getRegimen() != null ? n.getRegimen() : "");
        dirNegocio.setValue(n.getDireccion() != null ? n.getDireccion() : "");
        telNegocio.setValue(n.getTelefono() != null ? n.getTelefono() : "");
    }

    private void guardarValoresNegocio() {
        AppProperties.Negocio n = appProperties.getNegocio();
        n.setNombre(nombreNegocio.getValue());
        n.setRuc(rucNegocio.getValue());
        n.setRegimen(regimenNegocio.getValue());
        n.setDireccion(dirNegocio.getValue());
        n.setTelefono(telNegocio.getValue());

        Notificaciones.exito("Datos del negocio actualizados (en memoria).");
    }

    private VerticalLayout crearCardSri() {
        VerticalLayout layout = new VerticalLayout();
        layout.addClassName("card-blanca");
        layout.setWidth("100%");
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 titulo = new H3("Configuración SRI / Proveedor");
        titulo.getStyle().set("margin-top", "0").set("margin-bottom", "0.5rem");

        sriSimular.getStyle().set("margin-bottom", "0.25rem");
        sriBaseUrl.setPlaceholder("https://api.proveedor.com");
        sriBaseUrl.setWidthFull();
        sriToken.setPlaceholder("Token o API key");
        sriToken.setWidthFull();

        Span ayuda = new Span("Si \"Simular envío a SRI\" está activado, no se realiza llamada real al proveedor.");
        ayuda.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "var(--lumo-secondary-text-color)");

        Button guardar = new Button("Guardar SRI (en memoria)", e -> guardarValoresSri());
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout acciones = new HorizontalLayout(guardar);
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        acciones.setWidthFull();

        layout.add(titulo, sriSimular, sriBaseUrl, sriToken, ayuda, acciones);

        return layout;
    }

    private void cargarValoresSri() {
        AppProperties.Sri s = appProperties.getSri();
        sriSimular.setValue(s.isSimular());
        sriBaseUrl.setValue(s.getProveedorBaseUrl() != null ? s.getProveedorBaseUrl() : "");
        sriToken.setValue(s.getProveedorToken() != null ? s.getProveedorToken() : "");
    }

    private void guardarValoresSri() {
        AppProperties.Sri s = appProperties.getSri();
        s.setSimular(Boolean.TRUE.equals(sriSimular.getValue()));
        s.setProveedorBaseUrl(sriBaseUrl.getValue());
        s.setProveedorToken(sriToken.getValue());

        Notificaciones.exito("Configuración SRI actualizada (en memoria).");
    }

    private VerticalLayout crearCardBackup() {
        VerticalLayout layout = new VerticalLayout();
        layout.addClassName("card-blanca");
        layout.setWidthFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 titulo = new H3("Backups");
        titulo.getStyle().set("margin-top", "0").set("margin-bottom", "0.5rem");

        Span desc = new Span("Los backups copian la carpeta ./data/ a ./backups/ o a un dispositivo externo (USB).");
        desc.getStyle()
                .set("font-size", "0.85rem")
                .set("color", "var(--lumo-secondary-text-color)");

        usbPathField.setPlaceholder("Ejemplo Windows: E:\\\\    Ejemplo Linux: /media/usb");
        usbPathField.setWidth("320px");

        Button backupLocalBtn = new Button("Probar backup local (./backups/)", e -> probarBackupLocal());
        backupLocalBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button backupUsbBtn = new Button("Backup a USB", e -> probarBackupUsb());
        backupUsbBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        HorizontalLayout filaUsb = new HorizontalLayout(usbPathField, backupUsbBtn);
        filaUsb.setAlignItems(Alignment.END);
        filaUsb.setSpacing(true);

        layout.add(titulo, desc, backupLocalBtn, filaUsb);
        return layout;
    }

    private void probarBackupLocal() {
        try {
            var destino = backupService.crearBackupLocal(LocalDate.now());
            Notificaciones.exito("Backup local creado en: " + destino.toAbsolutePath());
        } catch (IOException ex) {
            Notificaciones.error("Error al crear backup local: " + ex.getMessage());
        }
    }

    private void probarBackupUsb() {
        String ruta = usbPathField.getValue();
        if (ruta == null || ruta.isBlank()) {
            Notificaciones.advertencia("Ingresa la ruta del USB antes de respaldar.");
            return;
        }

        try {
            Path usbRoot = Path.of(ruta.trim());
            var destino = backupService.crearBackupEnUsb(usbRoot, LocalDate.now());
            Notificaciones.exito("Backup en USB creado en: " + destino.toAbsolutePath());
        } catch (Exception ex) {
            Notificaciones.error("Error al respaldar a USB: " + ex.getMessage());
        }
    }
}
