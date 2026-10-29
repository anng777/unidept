package com.unihousing.service;

import com.unihousing.model.Alojamiento;
import com.unihousing.model.Ocupacion;
import com.unihousing.repository.AlojamientoRepository;
import com.unihousing.repository.OcupacionRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rangos de fechas ocupadas de un alojamiento (los que el calendario pinta con círculo rojo suave). */
@Service
@Transactional
public class OcupacionService {
    private final OcupacionRepository ocupaciones;
    private final AlojamientoRepository alojamientos;

    public OcupacionService(OcupacionRepository ocupaciones, AlojamientoRepository alojamientos) {
        this.ocupaciones = ocupaciones;
        this.alojamientos = alojamientos;
    }

    @Transactional(readOnly = true)
    public List<Ocupacion> listar(Alojamiento a) {
        return ocupaciones.findByAlojamientoOrderByFechaInicioAsc(a);
    }

    @Transactional(readOnly = true)
    public boolean hayChoque(Alojamiento a, LocalDate inicio, LocalDate fin) {
        return ocupaciones.existsByAlojamientoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(a, fin, inicio);
    }

    /** El arrendador marca manualmente un rango como ocupado. */
    public Ocupacion agregar(Integer idAlojamiento, Integer idArrendador, LocalDate inicio, LocalDate fin, String nota) {
        Alojamiento a = alojamientos.findById(idAlojamiento).orElseThrow(() -> new ReglaNegocioException("Alojamiento no encontrado."));
        if (!a.getArrendador().getIdUsuario().equals(idArrendador)) throw new ReglaNegocioException("Solo puedes gestionar tus propios alojamientos.");
        if (inicio == null || fin == null) throw new ReglaNegocioException("Indica la fecha de inicio y la de fin.");
        if (fin.isBefore(inicio)) throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la de inicio.");
        if (hayChoque(a, inicio, fin)) throw new ReglaNegocioException("Ese rango se cruza con fechas que ya están ocupadas.");
        Ocupacion o = new Ocupacion();
        o.setAlojamiento(a);
        o.setFechaInicio(inicio);
        o.setFechaFin(fin);
        o.setNota(nota == null || nota.isBlank() ? null : nota.trim());
        return ocupaciones.save(o);
    }

    public void eliminar(Integer idOcupacion, Integer idArrendador) {
        Ocupacion o = ocupaciones.findById(idOcupacion).orElseThrow(() -> new ReglaNegocioException("Rango no encontrado."));
        if (!o.getAlojamiento().getArrendador().getIdUsuario().equals(idArrendador)) throw new ReglaNegocioException("No autorizado.");
        ocupaciones.delete(o);
    }
}
