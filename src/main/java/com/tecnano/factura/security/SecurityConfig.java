package com.tecnano.factura.security;

import com.tecnano.factura.views.auth.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.net.URI;

@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Value("${app.security.h2-console-enabled:false}")
    private boolean h2ConsoleEnabled;

    @Value("${app.security.logout-get-enabled:false}")
    private boolean logoutGetEnabled;

    @Value("${app.security.csrf-ignore-api:true}")
    private boolean csrfIgnoreApi;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // ========= 0) RequestCache: NO guardes recursos estáticos como "continue" =========
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setRequestMatcher(this::shouldCacheRequest);
        http.requestCache(cache -> cache.requestCache(requestCache));

        // ========= 1) Publicar recursos estáticos (de verdad públicos) =========
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/img/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/images/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/icons/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/favicon.ico")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/robots.txt")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/manifest.webmanifest")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/sw.js")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/offline.html")).permitAll()
                // Vaadin típicos públicos (normalmente Vaadin ya los maneja, pero no estorba)
                .requestMatchers(new AntPathRequestMatcher("/VAADIN/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/frontend/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/webjars/**")).permitAll()
        );

        // ========= 2) CSRF: opcional ignorar /api/** =========
        if (csrfIgnoreApi) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/api/**")));
        }

        // ========= 3) H2 Console (solo si lo habilitas) =========
        if (h2ConsoleEnabled) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
            );
            http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")));
            http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }

        // ========= 4) Config base Vaadin =========
        super.configure(http);
        setLoginView(http, LoginView.class);

        // ========= 5) Login success: si "continue" era un recurso estático → /home =========
        http.formLogin(form -> form.successHandler((req, res, authentication) -> {
            SavedRequest saved = requestCache.getRequest(req, res);

            // Default POS: aterriza en Home
            String target = "/home";

            if (saved != null) {
                String redirectUrl = saved.getRedirectUrl();
                String path = safePath(redirectUrl);

                // Si NO es estático, respeta el redirect original
                if (path != null && !isStaticPath(path)) {
                    target = redirectUrl; // puede venir completo (http://localhost/...)
                } else {
                    // Si era estático, bórralo para que no persista
                    requestCache.removeRequest(req, res);
                }
            }

            // Redirigir
            res.sendRedirect(target);
        }));

        // ========= 6) Logout =========
        RequestMatcher logoutMatcher = logoutGetEnabled
                ? new OrRequestMatcher(
                new AntPathRequestMatcher("/logout", "POST"),
                new AntPathRequestMatcher("/logout", "GET")
        )
                : new AntPathRequestMatcher("/logout", "POST");

        http.logout(logout -> logout
                .logoutRequestMatcher(logoutMatcher)
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
        );
    }

    // Guarda solo páginas HTML reales; NO imágenes/css/js/etc
    private boolean shouldCacheRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return false;

        // No caches estáticos
        if (isStaticPath(uri)) return false;

        // Evitar caches de cosas internas / técnicas
        if (uri.startsWith("/VAADIN/") || uri.startsWith("/HILLA/")) return false;
        if (uri.startsWith("/UIDL/") || uri.startsWith("/HEARTBEAT/")) return false;
        if (uri.startsWith("/login") || uri.startsWith("/logout")) return false;

        // Solo cachea navegación normal (HTML)
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    private boolean isStaticPath(String path) {
        return path.startsWith("/img/")
                || path.startsWith("/images/")
                || path.startsWith("/icons/")
                || path.startsWith("/frontend/")
                || path.startsWith("/webjars/")
                || path.startsWith("/VAADIN/")
                || path.equals("/favicon.ico")
                || path.equals("/robots.txt")
                || path.equals("/manifest.webmanifest")
                || path.equals("/sw.js")
                || path.equals("/offline.html");
    }

    private String safePath(String redirectUrl) {
        try {
            return URI.create(redirectUrl).getPath();
        } catch (Exception e) {
            return null;
        }
    }
}
