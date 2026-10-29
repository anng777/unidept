package com.unihousing.web;

import com.unihousing.security.SesionUtil;
import com.unihousing.service.ReporteService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReporteController {
    private final ReporteService reportes;

    public ReporteController(ReporteService reportes) {
        this.reportes = reportes;
    }

    @PostMapping("/reportes")
    public String crear(@RequestParam(required = false) Integer idUsuario, @RequestParam(required = false) Integer idAlojamiento,
                        @RequestParam String motivo, RedirectAttributes ra) {
        reportes.crear(SesionUtil.requerido().getIdUsuario(), idUsuario, idAlojamiento, motivo);
        ra.addFlashAttribute("ok", "Reporte enviado. El administrador lo revisará.");
        return idAlojamiento != null ? "redirect:/alojamientos/" + idAlojamiento : "redirect:/alojamientos";
    }
}
