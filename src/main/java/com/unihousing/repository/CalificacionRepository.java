package com.unihousing.repository;

import com.unihousing.model.Calificacion;
import com.unihousing.model.Solicitud;
import com.unihousing.model.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalificacionRepository extends JpaRepository<Calificacion, Integer> {
    boolean existsBySolicitudAndCalificador(Solicitud solicitud, Usuario calificador);
    List<Calificacion> findByCalificadoOrderByFechaDescIdCalificacionDesc(Usuario calificado);
}
