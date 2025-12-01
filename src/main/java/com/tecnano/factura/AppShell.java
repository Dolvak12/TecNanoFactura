// =====================================================
// src/main/java/com/tecnano/factura/AppShell.java
// =====================================================
package com.tecnano.factura;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@PWA(
        name = "TecNanoFactura",
        shortName = "TecNanoFactura POS"
)
@Theme(value = "tecnano") // ✅ NO forces Lumo.LIGHT; así dark funciona limpio
@Meta(name = "viewport", content = "width=device-width, initial-scale=1, viewport-fit=cover")
@Meta(name = "theme-color", content = "#182132")
public class AppShell implements AppShellConfigurator {
}
