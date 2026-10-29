package com.unihousing.web;

import com.unihousing.model.EstadoPublicacion;
import com.unihousing.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Todo /admin/** exige ROLE_ADMINISTRADOR (ver SecurityConfig). */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UsuarioService usuarios;
    private final AlojamientoService alojamientos;
    private final ReporteService reportes;

    public AdminController(UsuarioService usuarios, AlojamientoService alojamientos, ReporteService reportes) {
        this.usuarios = usuarios;
        this.alojamientos = alojamientos;
        this.reportes = reportes;
    }

    @GetMapping
    public String panel(Model m) {
        m.addAttribute("totalUsuarios", usuarios.listarTodos().size());
        m.addAttribute("pendientes", alojamientos.contarPendientes());
        m.addAttribute("reportesPendientes", reportes.pendientes());
        return "admin/panel";
    }

    @GetMapping("/usuarios")
    public String listaUsuarios(Model m) {
        m.addAttribute("usuarios", usuarios.listarTodos());
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/activo")
    public String activo(@PathVariable Integer id, @RequestParam boolean valor, RedirectAttributes ra) {
        usuarios.cambiarActivo(id, valor);
        ra.addFlashAttribute("ok", valor ? "Usuario reactivado." : "Usuario suspendido.");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/publicaciones")
    public String publicaciones(@RequestParam(required = false) String estado, Model m) {
        EstadoPublicacion e = null;
        if (estado != null && !estado.isBlank()) {
            try { e = EstadoPublicacion.valueOf(estado); } catch (IllegalArgumentException ignored) { }
        }
        m.addAttribute("alojamientos", alojamientos.listarAdmin(e));
        m.addAttribute("estadoSel", estado);
        m.addAttribute("estados", EstadoPublicacion.values());
        return "admin/publicaciones";
    }

    @PostMapping("/publicaciones/{id}/estado")
    public String estado(@PathVariable Integer id, @RequestParam EstadoPublicacion estado, RedirectAttributes ra) {
        alojamientos.cambiarEstadoPublicacion(id, estado);
        ra.addFlashAttribute("ok", "Publicación actualizada: " + estado + ".");
        return "redirect:/admin/publicaciones";
    }

    @GetMapping("/reportes")
    public String listaReportes(Model m) {
        m.addAttribute("reportes", reportes.listar(null));
        return "admin/reportes";
    }

    @PostMapping("/reportes/{id}/revisado")
    public String revisado(@PathVariable Integer id, RedirectAttributes ra) {
        reportes.marcarRevisado(id);
        ra.addFlashAttribute("ok", "Reporte marcado como revisado.");
        return "redirect:/admin/reportes";
    }
}
