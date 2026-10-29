package com.unihousing.service;

import com.unihousing.model.*;
import com.unihousing.repository.CalificacionRepository;
import com.unihousing.repository.SolicitudRepository;
import com.unihousing.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CalificacionService {
    public record Resumen(double promedio, long total) {}

    private final CalificacionRepository calificaciones;
    private final SolicitudRepository solicitudes;
    private final UsuarioRepository usuarios;

    public CalificacionService(CalificacionRepository calificaciones, SolicitudRepository solicitudes, UsuarioRepository usuarios) {
        this.calificaciones = calificaciones;
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
    }

    /** Solo se califica tras una solicitud ACEPTADA, una vez por solicitud y por persona. */
    public Calificacion calificar(Integer idSolicitud, Integer idCalificador, int puntuacion, String comentario) {
        Solicitud s = solicitudes.findById(idSolicitud).orElseThrow(() -> new ReglaNegocioException("Solicitud no encontrada."));
        if (s.getEstado() != EstadoSolicitud.ACEPTADA) throw new ReglaNegocioException("Solo puedes calificar cuando la solicitud fue aceptada.");
        Usuario calificado;
        if (s.getEstudiante().getIdUsuario().equals(idCalificador)) calificado = s.getArrendador();
        else if (s.getArrendador().getIdUsuario().equals(idCalificador)) calificado = s.getEstudiante();
        else throw new ReglaNegocioException("No participas en esta solicitud.");
        if (puntuacion < 1 || puntuacion > 5) throw new ReglaNegocioException("La puntuación debe estar entre 1 y 5.");
        Usuario yo = usuarios.getReferenceById(idCalificador);
        if (calificaciones.existsBySolicitudAndCalificador(s, yo)) throw new ReglaNegocioException("Ya calificaste esta solicitud.");
        String c = comentario == null ? "" : comentario.trim();
        if (c.length() > 500) throw new ReglaNegocioException("El comentario es demasiado largo (máx. 500 caracteres).");
        Calificacion cal = new Calificacion();
        cal.setSolicitud(s);
        cal.setCalificador(yo);
        cal.setCalificado(calificado);
        cal.setPuntuacion(puntuacion);
        cal.setComentario(c.isEmpty() ? null : c);
        return calificaciones.save(cal);
    }

    @Transactional(readOnly = true)
    public boolean yaCalifico(Solicitud s, Integer idUsuario) {
        return calificaciones.existsBySolicitudAndCalificador(s, usuarios.getReferenceById(idUsuario));
    }

    @Transactional(readOnly = true)
    public List<Calificacion> deUsuario(Usuario u) {
        return calificaciones.findByCalificadoOrderByFechaDescIdCalificacionDesc(u);
    }

    @Transactional(readOnly = true)
    public Resumen resumen(Usuario u) {
        List<Calificacion> l = deUsuario(u);
        double prom = l.stream().mapToInt(Calificacion::getPuntuacion).average().orElse(0);
        return new Resumen(Math.round(prom * 10) / 10.0, l.size());
    }
}
