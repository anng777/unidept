package com.unihousing.service;

import com.unihousing.model.Mensaje;
import com.unihousing.model.Solicitud;
import com.unihousing.model.Usuario;
import com.unihousing.repository.MensajeRepository;
import com.unihousing.repository.SolicitudRepository;
import com.unihousing.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MensajeService {
    private final MensajeRepository mensajes;
    private final SolicitudRepository solicitudes;
    private final UsuarioRepository usuarios;

    public MensajeService(MensajeRepository mensajes, SolicitudRepository solicitudes, UsuarioRepository usuarios) {
        this.mensajes = mensajes;
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
    }

    /** Solo el estudiante y el arrendador de la solicitud participan en el chat. */
    public Solicitud verificarParticipante(Integer idSolicitud, Integer idUsuario) {
        Solicitud s = solicitudes.findById(idSolicitud).orElseThrow(() -> new ReglaNegocioException("Solicitud no encontrada."));
        boolean ok = s.getEstudiante().getIdUsuario().equals(idUsuario) || s.getArrendador().getIdUsuario().equals(idUsuario);
        if (!ok) throw new ReglaNegocioException("No participas en esta conversación.");
        return s;
    }

    /** Devuelve la conversación y marca como leídos los mensajes recibidos. */
    public List<Mensaje> conversacion(Integer idSolicitud, Integer idUsuario) {
        Solicitud s = verificarParticipante(idSolicitud, idUsuario);
        List<Mensaje> lista = mensajes.findBySolicitudOrderByFechaEnvioAscIdMensajeAsc(s);
        for (Mensaje m : lista) {
            if (!m.isLeido() && !m.getRemitente().getIdUsuario().equals(idUsuario)) {
                m.setLeido(true);
                mensajes.save(m);
            }
        }
        return lista;
    }

    public Mensaje enviar(Integer idSolicitud, Integer idUsuario, String contenido) {
        Solicitud s = verificarParticipante(idSolicitud, idUsuario);
        String texto = contenido == null ? "" : contenido.trim();
        if (texto.isEmpty()) throw new ReglaNegocioException("El mensaje no puede estar vacío.");
        if (texto.length() > 1000) throw new ReglaNegocioException("El mensaje es demasiado largo (máx. 1000 caracteres).");
        return guardar(s, usuarios.getReferenceById(idUsuario), texto);
    }

    /** Uso interno (primer mensaje de la solicitud, avisos de aceptación/rechazo). */
    public Mensaje guardar(Solicitud s, Usuario remitente, String texto) {
        Mensaje m = new Mensaje();
        m.setSolicitud(s);
        m.setRemitente(remitente);
        m.setContenido(texto);
        m.setLeido(false);
        return mensajes.save(m);
    }

    @Transactional(readOnly = true)
    public long noLeidos(Integer idUsuario) {
        return mensajes.contarNoLeidos(idUsuario);
    }

    @Transactional(readOnly = true)
    public long noLeidosDe(Solicitud s, Integer idUsuario) {
        return mensajes.contarNoLeidosDeSolicitud(s, idUsuario);
    }
}
