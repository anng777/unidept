package com.unihousing.web;

import com.unihousing.model.Solicitud;
import com.unihousing.security.SesionUtil;
import com.unihousing.service.CalificacionService;
import com.unihousing.service.MensajeService;
import com.unihousing.service.SolicitudService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SolicitudController {
    private final SolicitudService solicitudes;
    private final MensajeService mensajes;
    private final CalificacionService calificaciones;

    public SolicitudController(SolicitudService solicitudes, MensajeService mensajes, CalificacionService calificaciones) {
        this.solicitudes = solicitudes;
        this.mensajes = mensajes;
        this.calificaciones = calificaciones;
    }

    @PostMapping("/solicitudes/crear")
    public String crear(@RequestParam Integer idAlojamiento,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        @RequestParam(required = false) String mensaje,
                        @RequestParam(required = false) String metodoPago, RedirectAttributes ra) {
        solicitudes.crear(SesionUtil.requerido().getIdUsuario(), idAlojamiento, desde, hasta, mensaje, metodoPago);
        ra.addFlashAttribute("ok", "Solicitud enviada y reserva pagada. Podrás conversar con el arrendador desde aquí.");
        return "redirect:/mis-solicitudes";
    }

    @GetMapping("/mis-solicitudes")
    public String misSolicitudes(Model m) {
        Integer id = SesionUtil.requerido().getIdUsuario();
        List<Solicitud> lista = solicitudes.delEstudiante(id);
        Map<Integer, Long> noLeidos = new LinkedHashMap<>();
        for (Solicitud s : lista) noLeidos.put(s.getIdSolicitud(), mensajes.noLeidosDe(s, id));
        m.addAttribute("solicitudes", lista);
        m.addAttribute("noLeidos", noLeidos);
        return "mis-solicitudes";
    }
}
