package com.unihousing.web;

import com.unihousing.model.*;
import com.unihousing.security.SesionUtil;
import com.unihousing.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AlojamientoController {
    private final AlojamientoService alojamientos;
    private final OcupacionService ocupaciones;
    private final SolicitudService solicitudes;
    private final CalificacionService calificaciones;

    public AlojamientoController(AlojamientoService alojamientos, OcupacionService ocupaciones,
                                 SolicitudService solicitudes, CalificacionService calificaciones) {
        this.alojamientos = alojamientos;
        this.ocupaciones = ocupaciones;
        this.solicitudes = solicitudes;
        this.calificaciones = calificaciones;
    }

    /* ------------------------- Público ------------------------- */

    @GetMapping("/alojamientos")
    public String listado(@RequestParam(required = false) Integer universidad,
                          @RequestParam(required = false) String tipo,
                          @RequestParam(required = false) BigDecimal precioMin,
                          @RequestParam(required = false) BigDecimal precioMax,
                          @RequestParam(required = false) String distrito,
                          @RequestParam(defaultValue = "false") boolean roommates,
                          @RequestParam(defaultValue = "false") boolean mascotas,
                          @RequestParam(defaultValue = "false") boolean soloDisponibles,
                          @RequestParam(required = false) String orden,
                          Model m) {
        TipoAlojamiento t = null;
        if (tipo != null && !tipo.isBlank()) {
            try { t = TipoAlojamiento.valueOf(tipo); } catch (IllegalArgumentException ignored) { }
        }
        List<Alojamiento> lista;
        try {
            lista = alojamientos.buscar(t, precioMin, precioMax, distrito, roommates, mascotas, soloDisponibles);
        } catch (ReglaNegocioException e) {
            m.addAttribute("error", e.getMessage());
            lista = new ArrayList<>();
        }
        Usuario yo = SesionUtil.actual();
        boolean exacta = yo != null && (yo.isVerificado() || yo.esAdmin());
        Universidad uni = alojamientos.universidad(universidad);
        alojamientos.aplicarDistancia(lista, uni, exacta);
        alojamientos.ordenar(lista, orden, uni != null);

        m.addAttribute("alojamientos", lista);
        m.addAttribute("universidades", alojamientos.universidades());
        m.addAttribute("uniSel", universidad);
        m.addAttribute("tipoSel", tipo);
        m.addAttribute("precioMin", precioMin);
        m.addAttribute("precioMax", precioMax);
        m.addAttribute("distrito", distrito);
        m.addAttribute("roommates", roommates);
        m.addAttribute("mascotas", mascotas);
        m.addAttribute("soloDisponibles", soloDisponibles);
        m.addAttribute("orden", orden);
        m.addAttribute("tipos", TipoAlojamiento.values());
        return "listado";
    }

    @GetMapping("/alojamientos/{id:\\d+}")
    public String detalle(@PathVariable Integer id, @RequestParam(required = false) Integer uni, Model m) {
        Alojamiento a = alojamientos.obtener(id);
        Usuario yo = SesionUtil.actual();
        boolean propio = yo != null && yo.getIdUsuario().equals(a.getArrendador().getIdUsuario());
        if (a.getEstadoPublicacion() != EstadoPublicacion.APROBADA && !propio && (yo == null || !yo.esAdmin()))
            throw new ReglaNegocioException("Este alojamiento no está publicado.");
        boolean sensible = alojamientos.puedeVerSensible(yo, a);

        List<Alojamiento> uno = new ArrayList<>(List.of(a));
        alojamientos.marcarOcupacion(uno);
        // Sin universidad elegida se toma la más cercana, para que la distancia siempre tenga referencia.
        Universidad referencia = alojamientos.universidad(uni);
        if (referencia == null) {
            double mejor = Double.MAX_VALUE;
            for (Universidad u : alojamientos.universidades()) {
                double d = DistanciaService.haversine(a.getLatitud(), a.getLongitud(), u.getLatitud(), u.getLongitud());
                if (d < mejor) { mejor = d; referencia = u; }
            }
        }
        alojamientos.aplicarDistancia(uno, referencia, sensible);

        m.addAttribute("a", a);
        m.addAttribute("sensible", sensible);
        m.addAttribute("propio", propio);
        m.addAttribute("universidades", alojamientos.universidades());
        m.addAttribute("uniRef", referencia);
        m.addAttribute("esEstudiante", yo != null && yo.getTipoUsuario() == TipoUsuario.ESTUDIANTE);
        m.addAttribute("hoy", LocalDate.now());
        if (sensible) {
            Usuario arr = a.getArrendador();
            m.addAttribute("resumen", calificaciones.resumen(arr));
            m.addAttribute("resenas", calificaciones.deUsuario(arr));
        }
        return "detalle";
    }

    /* ------------------------- Arrendador ------------------------- */

    @GetMapping("/alojamientos/nuevo")
    public String nuevo(Model m) {
        m.addAttribute("form", new AlojamientoForm());
        m.addAttribute("accion", "/alojamientos/guardar");
        m.addAttribute("tipos", TipoAlojamiento.values());
        m.addAttribute("universidades", alojamientos.universidades());
        return "publicar";
    }

    @PostMapping("/alojamientos/guardar")
    public String guardar(@ModelAttribute AlojamientoForm form, RedirectAttributes ra) {
        alojamientos.crear(SesionUtil.requerido().getIdUsuario(), form);
        ra.addFlashAttribute("ok", "Alojamiento enviado. Será visible cuando el administrador lo apruebe.");
        return "redirect:/mis-alojamientos";
    }

    @GetMapping("/alojamientos/{id}/editar")
    public String editar(@PathVariable Integer id, Model m) {
        Alojamiento a = alojamientos.propio(id, SesionUtil.requerido().getIdUsuario());
        m.addAttribute("form", alojamientos.aFormulario(a));
        m.addAttribute("accion", "/alojamientos/" + id + "/editar");
        m.addAttribute("tipos", TipoAlojamiento.values());
        m.addAttribute("universidades", alojamientos.universidades());
        m.addAttribute("editando", true);
        return "publicar";
    }

    @PostMapping("/alojamientos/{id}/editar")
    public String actualizar(@PathVariable Integer id, @ModelAttribute AlojamientoForm form, RedirectAttributes ra) {
        alojamientos.actualizar(id, SesionUtil.requerido().getIdUsuario(), form);
        ra.addFlashAttribute("ok", "Cambios guardados. La publicación vuelve a revisión del administrador.");
        return "redirect:/mis-alojamientos";
    }

    @GetMapping("/mis-alojamientos")
    public String misAlojamientos(Model m) {
        Integer id = SesionUtil.requerido().getIdUsuario();
        m.addAttribute("alojamientos", alojamientos.delArrendador(id));
        m.addAttribute("solicitudes", solicitudes.recibidas(id));
        return "mis-alojamientos";
    }

    /** Interruptor Disponible / Ocupado. */
    @PostMapping("/mis-alojamientos/{id}/disponible")
    public String disponible(@PathVariable Integer id, @RequestParam boolean valor, RedirectAttributes ra) {
        alojamientos.cambiarDisponible(id, SesionUtil.requerido().getIdUsuario(), valor);
        ra.addFlashAttribute("ok", valor ? "Alojamiento marcado como DISPONIBLE." : "Alojamiento marcado como OCUPADO.");
        return "redirect:/mis-alojamientos";
    }

    @PostMapping("/mis-alojamientos/solicitudes/{id}/responder")
    public String responder(@PathVariable Integer id, @RequestParam boolean aceptar, RedirectAttributes ra) {
        solicitudes.responder(id, SesionUtil.requerido().getIdUsuario(), aceptar);
        ra.addFlashAttribute("ok", aceptar ? "Solicitud aceptada; las fechas quedaron marcadas como ocupadas." : "Solicitud rechazada.");
        return "redirect:/mis-alojamientos";
    }

    /* --------------- Calendario de ocupación (arrendador) --------------- */

    @GetMapping("/alojamientos/{id}/disponibilidad")
    public String disponibilidad(@PathVariable Integer id, Model m) {
        Alojamiento a = alojamientos.propio(id, SesionUtil.requerido().getIdUsuario());
        m.addAttribute("a", a);
        m.addAttribute("rangos", ocupaciones.listar(a));
        m.addAttribute("hoy", LocalDate.now());
        return "disponibilidad";
    }

    @PostMapping("/alojamientos/{id}/disponibilidad/agregar")
    public String agregarRango(@PathVariable Integer id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                               @RequestParam(required = false) String nota, RedirectAttributes ra) {
        ocupaciones.agregar(id, SesionUtil.requerido().getIdUsuario(), desde, hasta, nota);
        ra.addFlashAttribute("ok", "Fechas marcadas como ocupadas.");
        return "redirect:/alojamientos/" + id + "/disponibilidad";
    }

    @PostMapping("/alojamientos/{id}/disponibilidad/{idOcupacion}/eliminar")
    public String quitarRango(@PathVariable Integer id, @PathVariable Integer idOcupacion, RedirectAttributes ra) {
        ocupaciones.eliminar(idOcupacion, SesionUtil.requerido().getIdUsuario());
        ra.addFlashAttribute("ok", "Rango liberado.");
        return "redirect:/alojamientos/" + id + "/disponibilidad";
    }
}
