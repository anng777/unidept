package com.unihousing.web;

import com.unihousing.model.Ocupacion;
import com.unihousing.model.Usuario;
import com.unihousing.security.SesionUtil;
import com.unihousing.service.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final ReniecService reniec;
    private final UsuarioService usuarios;
    private final AlojamientoService alojamientos;
    private final OcupacionService ocupaciones;
    private final MensajeService mensajes;
    private final SolicitudService solicitudes;

    public ApiController(ReniecService reniec, UsuarioService usuarios, AlojamientoService alojamientos,
                         OcupacionService ocupaciones, MensajeService mensajes, SolicitudService solicitudes) {
        this.reniec = reniec;
        this.usuarios = usuarios;
        this.alojamientos = alojamientos;
        this.ocupaciones = ocupaciones;
        this.mensajes = mensajes;
        this.solicitudes = solicitudes;
    }

    /** Verifica el DNI en RENIEC antes de habilitar el registro. */
    @GetMapping("/dni")
    public Map<String, Object> dni(@RequestParam String dni) {
        Map<String, Object> r = new LinkedHashMap<>();
        if (!ValidacionUtil.esDniValido(dni)) {
            r.put("ok", false);
            r.put("mensaje", "El DNI debe tener 8 dígitos.");
            return r;
        }
        if (usuarios.existeDni(dni)) {
            r.put("ok", false);
            r.put("mensaje", "Ya existe una cuenta con este DNI.");
            return r;
        }
        ReniecService.ResultadoDni d = reniec.consultar(dni);
        r.put("ok", d.encontrado());
        if (d.encontrado()) {
            r.put("nombres", d.nombres());
            r.put("apellidos", d.apellidos());
        } else {
            r.put("mensaje", d.mensajeError() == null ? "No se encontró el DNI en RENIEC." : d.mensajeError());
        }
        return r;
    }

    /** Rangos ocupados (para el calendario). Solo fechas, sin datos personales. */
    @GetMapping("/alojamientos/{id}/ocupadas")
    public Map<String, Object> ocupadas(@PathVariable Integer id) {
        var a = alojamientos.obtener(id);
        List<Map<String, String>> rangos = new ArrayList<>();
        for (Ocupacion o : ocupaciones.listar(a)) {
            if (o.getFechaFin().isBefore(LocalDate.now())) continue;
            rangos.add(Map.of("desde", o.getFechaInicio().toString(), "hasta", o.getFechaFin().toString()));
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("disponible", a.isDisponible());
        r.put("rangos", rangos);
        return r;
    }

    @GetMapping("/notificaciones")
    public Map<String, Object> notificaciones() {
        Usuario u = SesionUtil.actual();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mensajes", u == null ? 0 : mensajes.noLeidos(u.getIdUsuario()));
        r.put("solicitudes", u == null || u.getTipoUsuario() != com.unihousing.model.TipoUsuario.ARRENDADOR ? 0 : solicitudes.pendientesRecibidas(u.getIdUsuario()));
        return r;
    }
}
