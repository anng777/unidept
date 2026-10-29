package com.unihousing.repository;

import com.unihousing.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmailIgnoreCaseOrNombreUsuarioIgnoreCase(String email, String nombreUsuario);
    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);
    boolean existsByDni(String dni);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);
}
