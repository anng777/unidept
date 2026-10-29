package com.unihousing.security;

import com.unihousing.model.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acceso al usuario autenticado por JWT (null si es visitante). */
public final class SesionUtil {
    private SesionUtil() {}

    public static Usuario actual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof UsuarioPrincipal p) return p.getUsuario();
        return null;
    }

    public static Usuario requerido() {
        Usuario u = actual();
        if (u == null) throw new com.unihousing.service.ReglaNegocioException("Debes iniciar sesión.");
        return u;
    }
}
