package com.unihousing.repository;

import com.unihousing.model.Mensaje;
import com.unihousing.model.Solicitud;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {
    List<Mensaje> findBySolicitudOrderByFechaEnvioAscIdMensajeAsc(Solicitud solicitud);

    @Query("select count(m) from Mensaje m where m.leido = false and m.remitente.idUsuario <> :uid "
         + "and (m.solicitud.estudiante.idUsuario = :uid or m.solicitud.alojamiento.arrendador.idUsuario = :uid)")
    long contarNoLeidos(@Param("uid") Integer idUsuario);

    @Query("select count(m) from Mensaje m where m.leido = false and m.solicitud = :s and m.remitente.idUsuario <> :uid")
    long contarNoLeidosDeSolicitud(@Param("s") Solicitud solicitud, @Param("uid") Integer idUsuario);
}
