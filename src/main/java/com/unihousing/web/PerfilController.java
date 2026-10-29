package com.unihousing.web;

import com.unihousing.model.Usuario;
import com.unihousing.security.SesionUtil;
import com.unihousing.service.CalificacionService;
import com.unihousing.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PerfilController {
    private final UsuarioService usuarios;
    private final CalificacionService calificaciones;

    public PerfilController(UsuarioService usuarios, CalificacionService calificaciones) {
        this.usuarios = usuarios;
        this.calificaciones = calificaciones;
    }

    @GetMapping("/perfil")
    public String perfil(Model m) {
        Usuario u = usuarios.obtener(SesionUtil.requerido().getIdUsuario());
        m.addAttribute("perfil", u);
        m.addAttribute("resumen", calificaciones.resumen(u));
        m.addAttribute("resenas", calificaciones.deUsuario(u));
        return "perfil";
    }

    @PostMapping("/perfil/usuario")
    public String cambiarUsuario(@RequestParam String nombreUsuario, RedirectAttributes ra) {
        usuarios.cambiarNombreUsuario(SesionUtil.requerido().getIdUsuario(), nombreUsuario);
        ra.addFlashAttribute("ok", "Nombre de usuario actualizado.");
        return "redirect:/perfil";
    }
}
