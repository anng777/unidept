package com.unihousing.repository;

import com.unihousing.model.Alojamiento;
import com.unihousing.model.Ocupacion;
import com.unihousing.model.Solicitud;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcupacionRepository extends JpaRepository<Ocupacion, Integer> {
    List<Ocupacion> findByAlojamientoOrderByFechaInicioAsc(Alojamiento alojamiento);

    /** Hay solape con [inicio, fin] si ocupacion.inicio <= fin Y ocupacion.fin >= inicio. */
    boolean existsByAlojamientoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(Alojamiento alojamiento, LocalDate fin, LocalDate inicio);

    List<Ocupacion> findBySolicitud(Solicitud solicitud);
}
