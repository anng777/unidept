package com.unihousing.web;

import com.unihousing.model.Solicitud;
import com.unihousing.model.Usuario;
import com.unihousing.security.SesionUtil;
import com.unihousing.service.CalificacionService;
import com.unihousing.service.MensajeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MensajeController {
    private final MensajeService mensajes;
    private final CalificacionService calificaciones;

    public MensajeController(MensajeService mensajes, CalificacionService calificaciones) {
        this.mensajes = mensajes;
        this.calificaciones = calificaciones;
    }

    @GetMapping("/chat/{id}")
    public String chat(@PathVariable Integer id, Model m) {
        Usuario yo = SesionUtil.requerido();
        Solicitud s = mensajes.verificarParticipante(id, yo.getIdUsuario());
        m.addAttribute("s", s);
        m.addAttribute("mensajes", mensajes.conversacion(id, yo.getIdUsuario()));
        m.addAttribute("otro", s.getEstudiante().getIdUsuario().equals(yo.getIdUsuario()) ? s.getArrendador() : s.getEstudiante());
        m.addAttribute("yaCalifico", calificaciones.yaCalifico(s, yo.getIdUsuario()));
        return "chat";
    }

    /** Fragmento que el JS refresca cada pocos segundos. */
    @GetMapping("/chat/{id}/mensajes")
    public String fragmento(@PathVariable Integer id, Model m) {
        Usuario yo = SesionUtil.requerido();
        m.addAttribute("mensajes", mensajes.conversacion(id, yo.getIdUsuario()));
        return "fragments/mensajes :: lista";
    }

    @PostMapping("/chat/{id}/enviar")
    public String enviar(@PathVariable Integer id, @RequestParam String contenido) {
        mensajes.enviar(id, SesionUtil.requerido().getIdUsuario(), contenido);
        return "redirect:/chat/" + id;
    }

    @PostMapping("/chat/{id}/calificar")
    public String calificar(@PathVariable Integer id, @RequestParam int puntuacion,
                            @RequestParam(required = false) String comentario, RedirectAttributes ra) {
        calificaciones.calificar(id, SesionUtil.requerido().getIdUsuario(), puntuacion, comentario);
        ra.addFlashAttribute("ok", "¡Gracias por tu calificación!");
        return "redirect:/chat/" + id;
    }
}
