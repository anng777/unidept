package com.unihousing.repository;

import com.unihousing.model.EstadoReporte;
import com.unihousing.model.Reporte;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReporteRepository extends JpaRepository<Reporte, Integer> {
    List<Reporte> findAllByOrderByIdReporteDesc();
    List<Reporte> findByEstadoOrderByIdReporteDesc(EstadoReporte estado);
    long countByEstado(EstadoReporte estado);
}
