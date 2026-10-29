package com.unihousing.repository;

import com.unihousing.model.Alojamiento;
import com.unihousing.model.EstadoSolicitud;
import com.unihousing.model.Solicitud;
import com.unihousing.model.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {
    List<Solicitud> findByEstudianteOrderByIdSolicitudDesc(Usuario estudiante);
    List<Solicitud> findByAlojamientoArrendadorOrderByIdSolicitudDesc(Usuario arrendador);
    boolean existsByEstudianteAndAlojamientoAndEstadoIn(Usuario estudiante, Alojamiento alojamiento, Collection<EstadoSolicitud> estados);
}
