package com.tecnano.factura.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    public static final String LOGOUT_SUCCESS_URL = "/login";

    public void logout() {
        var request = VaadinServletRequest.getCurrent().getHttpServletRequest();
        var response = VaadinServletResponse.getCurrent().getHttpServletResponse();
        var auth = SecurityContextHolder.getContext().getAuthentication();

        // 1) Invalida sesión + limpia SecurityContext
        new SecurityContextLogoutHandler().logout(request, response, auth);

        // 2) Redirección (recarga completa)
        UI.getCurrent().getPage().setLocation(LOGOUT_SUCCESS_URL);
    }
}
