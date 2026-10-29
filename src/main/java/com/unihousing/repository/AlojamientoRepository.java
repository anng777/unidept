package com.unihousing.repository;

import com.unihousing.model.Alojamiento;
import com.unihousing.model.EstadoPublicacion;
import com.unihousing.model.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlojamientoRepository extends JpaRepository<Alojamiento, Integer>, JpaSpecificationExecutor<Alojamiento> {
    List<Alojamiento> findByArrendadorOrderByIdAlojamientoDesc(Usuario arrendador);
    List<Alojamiento> findByEstadoPublicacionOrderByIdAlojamientoDesc(EstadoPublicacion estado);
    List<Alojamiento> findAllByOrderByIdAlojamientoDesc();
}
