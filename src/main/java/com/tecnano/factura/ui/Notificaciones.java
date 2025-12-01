// =====================================================
// src/main/java/com/tecnano/factura/ui/Notificaciones.java
// =====================================================
package com.tecnano.factura.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public final class Notificaciones {

    private static final int DURACION_CORTA = 2500;
    private static final int DURACION_MEDIA = 4000;

    private Notificaciones() { }

    public static void info(String mensaje) {
        Notification n = new Notification(mensaje, DURACION_CORTA, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        n.open();
    }

    public static void exito(String mensaje) {
        Notification n = new Notification(mensaje, DURACION_CORTA, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        n.open();
    }

    public static void advertencia(String mensaje) {
        Notification n = new Notification(mensaje, DURACION_MEDIA, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        n.open();
    }

    public static void error(String mensaje) {
        Notification n = new Notification(mensaje, DURACION_MEDIA, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        n.open();
    }

    public static void errorLargo(String titulo, String detalle) {
        Notification notification = new Notification();
        notification.setPosition(Notification.Position.TOP_CENTER);
        notification.setDuration(0);

        Span titleSpan = new Span(titulo);
        titleSpan.getStyle()
                .set("font-weight", "600")
                .set("margin-bottom", "0.15rem");

        Span detailSpan = new Span(detalle);
        detailSpan.getStyle()
                .set("font-size", "0.85rem");

        Button close = new Button("Cerrar", e -> notification.close());
        close.getStyle().set("margin-left", "0.75rem");

        com.vaadin.flow.component.orderedlayout.VerticalLayout content =
                new com.vaadin.flow.component.orderedlayout.VerticalLayout(titleSpan, detailSpan);
        content.setPadding(false);
        content.setSpacing(false);

        com.vaadin.flow.component.orderedlayout.HorizontalLayout layout =
                new com.vaadin.flow.component.orderedlayout.HorizontalLayout(content, close);
        layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        layout.setPadding(false);
        layout.setSpacing(true);

        notification.add(layout);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }
}
