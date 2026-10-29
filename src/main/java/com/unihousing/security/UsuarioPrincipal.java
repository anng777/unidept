package com.unihousing.security;

import com.unihousing.model.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Adaptador entre la entidad Usuario y Spring Security. El rol real es ROLE_ESTUDIANTE / ROLE_ARRENDADOR / ROLE_ADMINISTRADOR. */
public class UsuarioPrincipal implements UserDetails {
    private final Usuario usuario;

    public UsuarioPrincipal(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() { return usuario; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getTipoUsuario().name()));
    }

    @Override public String getPassword() { return usuario.getPasswordHash(); }
    @Override public String getUsername() { return usuario.getEmail(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    /** Una cuenta suspendida por el administrador no puede iniciar sesión. */
    @Override public boolean isEnabled() { return usuario.isActivo(); }
}
