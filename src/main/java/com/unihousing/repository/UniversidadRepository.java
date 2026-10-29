package com.unihousing.repository;

import com.unihousing.model.Universidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversidadRepository extends JpaRepository<Universidad, Integer> {
    List<Universidad> findAllByOrderByNombreAsc();
}
