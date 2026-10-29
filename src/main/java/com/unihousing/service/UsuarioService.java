package com.unihousing.service;

import com.unihousing.model.TipoUsuario;
import com.unihousing.model.Usuario;
import com.unihousing.repository.UsuarioRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final ReniecService reniec;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder, ReniecService reniec) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.reniec = reniec;
    }

    /** Crea la cuenta SOLO si RENIEC verifica el DNI y ese DNI no tiene otra cuenta. */
    public Usuario registrar(RegistroForm f) {
        String dni = limpio(f.getDni());
        String email = limpio(f.getEmail());
        String usuario = limpio(f.getNombreUsuario());
        String telefono = limpio(f.getTelefono());
        String clave = f.getPassword() == null ? "" : f.getPassword();

        TipoUsuario tipo;
        try {
            tipo = TipoUsuario.valueOf(limpio(f.getTipoUsuario()));
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Tipo de cuenta inválido.");
        }
        if (tipo == TipoUsuario.ADMINISTRADOR) throw new ReglaNegocioException("Tipo de cuenta inválido.");
        if (!ValidacionUtil.esDniValido(dni)) throw new ReglaNegocioException("El DNI debe tener 8 dígitos.");
        if (!ValidacionUtil.esNombreUsuarioValido(usuario))
            throw new ReglaNegocioException("El usuario debe tener 4 a 20 caracteres (letras, números, punto o guion bajo).");
        if (!ValidacionUtil.esEmailValido(email)) throw new ReglaNegocioException("Correo electrónico inválido.");
        if (tipo == TipoUsuario.ESTUDIANTE && !ValidacionUtil.esCorreoInstitucional(email))
            throw new ReglaNegocioException("Los estudiantes deben usar un correo institucional (.edu.pe o .edu).");
        if (!ValidacionUtil.esCelularValido(telefono)) throw new ReglaNegocioException("El celular debe empezar con 9 y tener 9 dígitos.");
        if (clave.length() < 8 || !clave.matches(".*[A-Za-z].*") || !clave.matches(".*\\d.*"))
            throw new ReglaNegocioException("La contraseña debe tener mínimo 8 caracteres, con letras y números.");

        if (usuarios.existsByDni(dni)) throw new ReglaNegocioException("Ya existe una cuenta registrada con este DNI.");
        ReniecService.ResultadoDni r = reniec.consultar(dni);
        if (!r.encontrado()) {
            throw new ReglaNegocioException("No se pudo verificar tu DNI en RENIEC. " + (r.mensajeError() == null ? "" : r.mensajeError()));
        }
        if (usuarios.existsByEmailIgnoreCase(email)) throw new ReglaNegocioException("Ese correo ya está registrado.");
        if (usuarios.existsByNombreUsuarioIgnoreCase(usuario)) throw new ReglaNegocioException("Ese nombre de usuario ya está en uso.");

        Usuario u = new Usuario();
        u.setDni(dni);
        u.setNombreUsuario(usuario);
        u.setNombres(r.nombres());
        u.setApellidos(r.apellidos());
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(encoder.encode(clave));
        u.setTelefono(telefono);
        u.setTipoUsuario(tipo);
        u.setVerificado(true);
        u.setActivo(true);
        try {
            return usuarios.saveAndFlush(u);
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("El DNI, correo o usuario ya están registrados.");
        }
    }

    public Usuario cambiarNombreUsuario(Integer idUsuario, String nuevo) {
        nuevo = limpio(nuevo);
        if (!ValidacionUtil.esNombreUsuarioValido(nuevo))
            throw new ReglaNegocioException("El usuario debe tener 4 a 20 caracteres (letras, números, punto o guion bajo).");
        Usuario u = obtener(idUsuario);
        usuarios.findByNombreUsuarioIgnoreCase(nuevo).ifPresent(otro -> {
            if (!otro.getIdUsuario().equals(idUsuario)) throw new ReglaNegocioException("Ese nombre de usuario ya está en uso.");
        });
        u.setNombreUsuario(nuevo);
        return usuarios.save(u);
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Integer id) {
        return usuarios.findById(id).orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado."));
    }

    @Transactional(readOnly = true)
    public boolean existeDni(String dni) {
        return usuarios.existsByDni(dni);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarios.findAll(org.springframework.data.domain.Sort.by("idUsuario"));
    }

    /** Suspender / reactivar (solo administrador; no se puede suspender a otro administrador). */
    public void cambiarActivo(Integer idUsuario, boolean activo) {
        Usuario u = obtener(idUsuario);
        if (u.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) throw new ReglaNegocioException("No puedes suspender a un administrador.");
        u.setActivo(activo);
        usuarios.save(u);
    }

    private static String limpio(String s) {
        return s == null ? "" : s.trim();
    }
}
