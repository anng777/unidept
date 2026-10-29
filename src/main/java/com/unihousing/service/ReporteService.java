package com.unihousing.service;

import com.unihousing.model.*;
import com.unihousing.repository.AlojamientoRepository;
import com.unihousing.repository.ReporteRepository;
import com.unihousing.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReporteService {
    private final ReporteRepository reportes;
    private final UsuarioRepository usuarios;
    private final AlojamientoRepository alojamientos;

    public ReporteService(ReporteRepository reportes, UsuarioRepository usuarios, AlojamientoRepository alojamientos) {
        this.reportes = reportes;
        this.usuarios = usuarios;
        this.alojamientos = alojamientos;
    }

    /** Se reporta un alojamiento y/o un usuario (al menos uno). */
    public Reporte crear(Integer idReportante, Integer idUsuarioReportado, Integer idAlojamientoReportado, String motivo) {
        String m = motivo == null ? "" : motivo.trim();
        if (m.length() < 10) throw new ReglaNegocioException("Describe el motivo con al menos 10 caracteres.");
        if (m.length() > 1000) throw new ReglaNegocioException("El motivo es demasiado largo (máx. 1000 caracteres).");
        if (idUsuarioReportado == null && idAlojamientoReportado == null) throw new ReglaNegocioException("Indica qué estás reportando.");
        if (idUsuarioReportado != null && idUsuarioReportado.equals(idReportante)) throw new ReglaNegocioException("No puedes reportarte a ti mismo.");
        Reporte r = new Reporte();
        r.setReportante(usuarios.getReferenceById(idReportante));
        if (idUsuarioReportado != null)
            r.setUsuarioReportado(usuarios.findById(idUsuarioReportado).orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado.")));
        if (idAlojamientoReportado != null)
            r.setAlojamientoReportado(alojamientos.findById(idAlojamientoReportado).orElseThrow(() -> new ReglaNegocioException("Alojamiento no encontrado.")));
        r.setMotivo(m);
        r.setEstado(EstadoReporte.PENDIENTE);
        return reportes.save(r);
    }

    @Transactional(readOnly = true)
    public List<Reporte> listar(EstadoReporte estado) {
        return estado == null ? reportes.findAllByOrderByIdReporteDesc() : reportes.findByEstadoOrderByIdReporteDesc(estado);
    }

    public void marcarRevisado(Integer id) {
        Reporte r = reportes.findById(id).orElseThrow(() -> new ReglaNegocioException("Reporte no encontrado."));
        r.setEstado(EstadoReporte.REVISADO);
        reportes.save(r);
    }

    @Transactional(readOnly = true)
    public long pendientes() {
        return reportes.countByEstado(EstadoReporte.PENDIENTE);
    }
}
