package com.unihousing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Defensa anti-CSRF por origen: en peticiones que modifican datos (POST, PUT, DELETE, PATCH) hechas desde el
 * navegador, el Origin (o el Referer) debe ser el mismo sitio. Las llamadas con "Authorization: Bearer" no se
 * verifican (no usan cookies).
 */
public class OrigenFilter extends OncePerRequestFilter {
    private static final Set<String> METODOS = Set.of("POST", "PUT", "DELETE", "PATCH");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (METODOS.contains(req.getMethod()) && req.getHeader("Authorization") == null) {
            String origin = req.getHeader("Origin");
            String fuente = origin != null && !"null".equals(origin) ? origin : req.getHeader("Referer");
            if (fuente != null && !fuente.isBlank()) {
                String host = req.getHeader("Host");
                String autoridad;
                try {
                    autoridad = URI.create(fuente).getAuthority();
                } catch (IllegalArgumentException e) {
                    autoridad = null;
                }
                if (autoridad == null || host == null || !autoridad.equalsIgnoreCase(host)) {
                    logger.warn("POST rechazado por origen distinto: origen=" + fuente + " host=" + host);
                    res.sendError(403, "Origen no permitido");
                    return;
                }
            }
        }
        chain.doFilter(req, res);
    }
}
