package com.unihousing.service;

import com.unihousing.model.*;
import com.unihousing.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SolicitudService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Comision de reserva cobrada al enviar la solicitud (10% del precio mensual). */
    public static final double PORCENTAJE_RESERVA = 0.10;
    private static final Set<String> METODOS_PAGO = Set.of("TARJETA", "YAPE", "PLIN", "TRANSFERENCIA");

    private final SolicitudRepository solicitudes;
    private final AlojamientoRepository alojamientos;
    private final UsuarioRepository usuarios;
    private final OcupacionRepository ocupaciones;
    private final MensajeService mensajes;

    public SolicitudService(SolicitudRepository solicitudes, AlojamientoRepository alojamientos, UsuarioRepository usuarios,
                            OcupacionRepository ocupaciones, MensajeService mensajes) {
        this.solicitudes = solicitudes;
        this.alojamientos = alojamientos;
        this.usuarios = usuarios;
        this.ocupaciones = ocupaciones;
        this.mensajes = mensajes;
    }

    /** Monto de reserva para un alojamiento (10% del precio mensual, redondeado a 2 decimales). */
    public static BigDecimal montoReserva(Alojamiento a) {
        if (a.getPrecioMensual() == null) return BigDecimal.ZERO;
        return a.getPrecioMensual().multiply(BigDecimal.valueOf(PORCENTAJE_RESERVA)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Solo estudiantes solicitan; las fechas no pueden chocar con rangos ocupados. */
    public Solicitud crear(Integer idEstudiante, Integer idAlojamiento, LocalDate inicio, LocalDate fin, String mensaje, String metodoPago) {
        Usuario est = usuarios.findById(idEstudiante).orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado."));
        if (est.getTipoUsuario() != TipoUsuario.ESTUDIANTE) throw new ReglaNegocioException("Solo los estudiantes pueden solicitar alojamientos.");
        if (!est.isVerificado()) throw new ReglaNegocioException("Tu cuenta debe estar verificada para solicitar.");
        Alojamiento a = alojamientos.findById(idAlojamiento).orElseThrow(() -> new ReglaNegocioException("Alojamiento no encontrado."));
        if (a.getEstadoPublicacion() != EstadoPublicacion.APROBADA) throw new ReglaNegocioException("Este alojamiento no está disponible para solicitudes.");
        if (!a.isDisponible()) throw new ReglaNegocioException("El arrendador marcó este alojamiento como ocupado.");
        if (inicio == null || fin == null) throw new ReglaNegocioException("Selecciona la fecha de inicio y la de fin.");
        if (inicio.isBefore(LocalDate.now())) throw new ReglaNegocioException("La fecha de inicio no puede estar en el pasado.");
        if (fin.isBefore(inicio)) throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la de inicio.");
        if (ocupaciones.existsByAlojamientoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(a, fin, inicio))
            throw new ReglaNegocioException("El alojamiento está ocupado en alguna de las fechas elegidas (marcadas en rojo en el calendario).");
        if (solicitudes.existsByEstudianteAndAlojamientoAndEstadoIn(est, a, Set.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.ACEPTADA)))
            throw new ReglaNegocioException("Ya tienes una solicitud activa para este alojamiento.");
        String texto = mensaje == null ? "" : mensaje.trim();
        if (texto.length() > 500) throw new ReglaNegocioException("El mensaje es demasiado largo (máx. 500 caracteres).");
        String metodo = metodoPago == null ? "" : metodoPago.trim().toUpperCase();
        if (!METODOS_PAGO.contains(metodo)) throw new ReglaNegocioException("Selecciona un método de pago válido para reservar.");

        Solicitud s = new Solicitud();
        s.setEstudiante(est);
        s.setAlojamiento(a);
        s.setFechaInicio(inicio);
        s.setFechaFin(fin);
        s.setMensaje(texto.isEmpty() ? null : texto);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        // Pago simulado de la reserva
        s.setMontoPago(montoReserva(a));
        s.setMetodoPago(metodo);
        s.setEstadoPago("PAGADO");
        s.setFechaPago(LocalDateTime.now());
        s = solicitudes.saveAndFlush(s);
        if (!texto.isEmpty()) mensajes.guardar(s, est, texto); // el primer mensaje aparece en el chat
        return s;
    }

    @Transactional(readOnly = true)
    public List<Solicitud> delEstudiante(Integer idEstudiante) {
        return solicitudes.findByEstudianteOrderByIdSolicitudDesc(usuarios.getReferenceById(idEstudiante));
    }

    @Transactional(readOnly = true)
    public List<Solicitud> recibidas(Integer idArrendador) {
        return solicitudes.findByAlojamientoArrendadorOrderByIdSolicitudDesc(usuarios.getReferenceById(idArrendador));
    }

    @Transactional(readOnly = true)
    public long pendientesRecibidas(Integer idArrendador) {
        return recibidas(idArrendador).stream().filter(s -> s.getEstado() == EstadoSolicitud.PENDIENTE).count();
    }

    /** Aceptar crea automáticamente el rango ocupado; rechazar solo cambia el estado. */
    public Solicitud responder(Integer idSolicitud, Integer idArrendador, boolean aceptar) {
        Solicitud s = solicitudes.findById(idSolicitud).orElseThrow(() -> new ReglaNegocioException("Solicitud no encontrada."));
        if (!s.getArrendador().getIdUsuario().equals(idArrendador)) throw new ReglaNegocioException("Solo el arrendador puede responder.");
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) throw new ReglaNegocioException("Esta solicitud ya fue respondida.");
        Usuario arr = usuarios.getReferenceById(idArrendador);
        String rango = s.getFechaInicio() == null ? "" : " del " + s.getFechaInicio().format(FMT) + " al " + s.getFechaFin().format(FMT);
        if (aceptar) {
            if (s.getFechaInicio() != null && ocupaciones.existsByAlojamientoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                    s.getAlojamiento(), s.getFechaFin(), s.getFechaInicio()))
                throw new ReglaNegocioException("No se puede aceptar: esas fechas ya están ocupadas.");
            s.setEstado(EstadoSolicitud.ACEPTADA);
            if (s.getFechaInicio() != null) {
                Ocupacion o = new Ocupacion();
                o.setAlojamiento(s.getAlojamiento());
                o.setFechaInicio(s.getFechaInicio());
                o.setFechaFin(s.getFechaFin());
                o.setNota("Reserva de " + s.getEstudiante().getNombreUsuario());
                o.setSolicitud(s);
                ocupaciones.save(o);
            }
            mensajes.guardar(s, arr, "¡Tu solicitud fue aceptada" + rango + "! Coordinemos los detalles por aquí.");
        } else {
            s.setEstado(EstadoSolicitud.RECHAZADA);
            mensajes.guardar(s, arr, "Lamentablemente no puedo aceptar tu solicitud" + rango + ".");
        }
        return solicitudes.save(s);
    }
}
