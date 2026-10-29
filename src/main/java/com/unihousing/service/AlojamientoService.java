package com.unihousing.service;

import com.unihousing.model.*;
import com.unihousing.repository.AlojamientoRepository;
import com.unihousing.repository.OcupacionRepository;
import com.unihousing.repository.UniversidadRepository;
import com.unihousing.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AlojamientoService {
    // Lima Metropolitana + Callao (la plataforma es para universidades de Lima)
    private static final double LAT_MIN = -12.55, LAT_MAX = -11.70, LNG_MIN = -77.25, LNG_MAX = -76.60;

    private final AlojamientoRepository alojamientos;
    private final UniversidadRepository universidades;
    private final UsuarioRepository usuarios;
    private final OcupacionRepository ocupaciones;
    private final DistanciaService distancias;

    public AlojamientoService(AlojamientoRepository alojamientos, UniversidadRepository universidades, UsuarioRepository usuarios,
                              OcupacionRepository ocupaciones, DistanciaService distancias) {
        this.alojamientos = alojamientos;
        this.universidades = universidades;
        this.usuarios = usuarios;
        this.ocupaciones = ocupaciones;
        this.distancias = distancias;
    }

    /* ---------------------- Búsqueda pública ---------------------- */

    @Transactional(readOnly = true)
    public List<Alojamiento> buscar(TipoAlojamiento tipo, BigDecimal precioMin, BigDecimal precioMax, String distrito,
                                    boolean roommates, boolean mascotas, boolean soloDisponibles) {
        if ((precioMin != null && precioMin.signum() < 0) || (precioMax != null && precioMax.signum() < 0))
            throw new ReglaNegocioException("Los precios deben ser 0 o mayores.");
        if (precioMin != null && precioMax != null && precioMin.compareTo(precioMax) > 0)
            throw new ReglaNegocioException("El precio mínimo no puede ser mayor al máximo.");

        Specification<Alojamiento> spec = (r, q, cb) -> cb.equal(r.get("estadoPublicacion"), EstadoPublicacion.APROBADA);
        if (tipo != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("tipo"), tipo));
        if (precioMin != null) spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.<BigDecimal>get("precioMensual"), precioMin));
        if (precioMax != null) spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.<BigDecimal>get("precioMensual"), precioMax));
        if (distrito != null && !distrito.isBlank()) {
            String patron = "%" + distrito.trim().toLowerCase() + "%";
            spec = spec.and((r, q, cb) -> cb.like(cb.lower(r.<String>get("distrito")), patron));
        }
        if (roommates) spec = spec.and((r, q, cb) -> cb.isTrue(r.<Boolean>get("admiteRoommates")));
        if (mascotas) spec = spec.and((r, q, cb) -> cb.isTrue(r.<Boolean>get("admiteMascotas")));

        List<Alojamiento> lista = new ArrayList<>(alojamientos.findAll(spec, Sort.by(Sort.Direction.DESC, "idAlojamiento")));
        marcarOcupacion(lista);
        if (soloDisponibles) lista.removeIf(Alojamiento::isOcupadoAhora);
        return lista;
    }

    public void ordenar(List<Alojamiento> lista, String orden, boolean hayUniversidad) {
        if ("precio_asc".equals(orden)) lista.sort(Comparator.comparing(Alojamiento::getPrecioMensual));
        else if ("precio_desc".equals(orden)) lista.sort(Comparator.comparing(Alojamiento::getPrecioMensual).reversed());
        else if (hayUniversidad && (orden == null || orden.isBlank() || "distancia".equals(orden)))
            lista.sort(Comparator.comparing(Alojamiento::getDistanciaMetros, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * Calcula distancia y tiempo hacia la universidad. Para quien no tiene cuenta verificada la distancia se
     * redondea a 100 m (así no se puede deducir la ubicación exacta a partir de varias universidades).
     */
    public void aplicarDistancia(List<Alojamiento> lista, Universidad uni, boolean exacta) {
        if (uni == null) return;
        for (Alojamiento a : lista) {
            DistanciaService.ResultadoDistancia d = distancias.calcular(a.getLatitud(), a.getLongitud(), uni.getLatitud(), uni.getLongitud());
            double metros = exacta ? d.metros() : Math.max(100, Math.round(d.metros() / 100.0) * 100.0);
            int minutos = exacta ? d.minutos() : Math.max(1, (int) Math.round(metros / DistanciaService.METROS_POR_MINUTO));
            a.setUniversidadReferencia(uni.getNombre());
            a.setDistanciaMetros(metros);
            a.setTiempoMinutos(minutos);
        }
    }

    /** Marca cada alojamiento como OCUPADO si el arrendador lo puso en "ocupado" o si hoy cae en un rango ocupado. */
    @Transactional(readOnly = true)
    public void marcarOcupacion(List<Alojamiento> lista) {
        LocalDate hoy = LocalDate.now();
        for (Alojamiento a : lista) {
            a.setOcupadoAhora(!a.isDisponible()
                    || ocupaciones.existsByAlojamientoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(a, hoy, hoy));
        }
    }

    @Transactional(readOnly = true)
    public List<Universidad> universidades() {
        return universidades.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Universidad universidad(Integer id) {
        return id == null ? null : universidades.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Alojamiento obtener(Integer id) {
        return alojamientos.findById(id).orElseThrow(() -> new ReglaNegocioException("Alojamiento no encontrado."));
    }

    /** ¿Puede el usuario ver la información sensible (dirección exacta, mapa, perfil del arrendador)? */
    public boolean puedeVerSensible(Usuario u, Alojamiento a) {
        if (u == null) return false;
        return u.isVerificado() || u.esAdmin() || u.getIdUsuario().equals(a.getArrendador().getIdUsuario());
    }

    /* ---------------------- Arrendador ---------------------- */

    @Transactional(readOnly = true)
    public List<Alojamiento> delArrendador(Integer idArrendador) {
        List<Alojamiento> lista = new ArrayList<>(alojamientos.findByArrendadorOrderByIdAlojamientoDesc(usuarios.getReferenceById(idArrendador)));
        marcarOcupacion(lista);
        return lista;
    }

    public Alojamiento crear(Integer idArrendador, AlojamientoForm f) {
        Usuario dueno = usuarios.findById(idArrendador).orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado."));
        if (dueno.getTipoUsuario() != TipoUsuario.ARRENDADOR) throw new ReglaNegocioException("Solo los arrendadores pueden publicar.");
        Alojamiento a = new Alojamiento();
        a.setArrendador(dueno);
        aplicar(a, f);
        a.setEstadoPublicacion(EstadoPublicacion.PENDIENTE);
        a.setDisponible(true);
        a = alojamientos.saveAndFlush(a);
        List<String> fotos = urlsDe(f.getImagenes());
        if (fotos.isEmpty()) {
            for (int n = 1; n <= 3; n++) fotos.add("https://picsum.photos/seed/alojamiento" + a.getIdAlojamiento() + "-" + n + "/700/500");
        }
        a.reemplazarImagenes(fotos);
        return alojamientos.save(a);
    }

    /** El arrendador dueño edita TODA la información. La publicación vuelve a revisión. */
    public Alojamiento actualizar(Integer idAlojamiento, Integer idArrendador, AlojamientoForm f) {
        Alojamiento a = propio(idAlojamiento, idArrendador);
        aplicar(a, f);
        a.setDisponible(f.isDisponible());
        a.setEstadoPublicacion(EstadoPublicacion.PENDIENTE);
        List<String> fotos = urlsDe(f.getImagenes());
        if (!fotos.isEmpty()) a.reemplazarImagenes(fotos);
        return alojamientos.save(a);
    }

    /** Interruptor Disponible / Ocupado controlado por el arrendador. */
    public Alojamiento cambiarDisponible(Integer idAlojamiento, Integer idArrendador, boolean disponible) {
        Alojamiento a = propio(idAlojamiento, idArrendador);
        a.setDisponible(disponible);
        return alojamientos.save(a);
    }

    public Alojamiento propio(Integer idAlojamiento, Integer idArrendador) {
        Alojamiento a = obtener(idAlojamiento);
        if (!a.getArrendador().getIdUsuario().equals(idArrendador)) throw new ReglaNegocioException("Solo puedes gestionar tus propios alojamientos.");
        return a;
    }

    public AlojamientoForm aFormulario(Alojamiento a) {
        AlojamientoForm f = new AlojamientoForm();
        f.setTitulo(a.getTitulo());
        f.setDescripcion(a.getDescripcion());
        f.setTipo(a.getTipo().name());
        f.setPrecioMensual(a.getPrecioMensual());
        f.setDireccion(a.getDireccion());
        f.setDistrito(a.getDistrito());
        f.setLatitud(a.getLatitud());
        f.setLongitud(a.getLongitud());
        f.setCapacidad(a.getCapacidad());
        f.setAmoblado(a.isAmoblado());
        f.setWifi(a.isWifi());
        f.setAdmiteRoommates(a.isAdmiteRoommates());
        f.setAdmiteMascotas(a.isAdmiteMascotas());
        f.setDisponible(a.isDisponible());
        f.setImagenes(String.join("\n", a.getUrlsImagenes()));
        return f;
    }

    /* ---------------------- Administrador ---------------------- */

    @Transactional(readOnly = true)
    public List<Alojamiento> listarAdmin(EstadoPublicacion estado) {
        return estado == null ? alojamientos.findAllByOrderByIdAlojamientoDesc() : alojamientos.findByEstadoPublicacionOrderByIdAlojamientoDesc(estado);
    }

    public void cambiarEstadoPublicacion(Integer idAlojamiento, EstadoPublicacion estado) {
        Alojamiento a = obtener(idAlojamiento);
        a.setEstadoPublicacion(estado);
        alojamientos.save(a);
    }

    @Transactional(readOnly = true)
    public long contarPendientes() {
        return alojamientos.findByEstadoPublicacionOrderByIdAlojamientoDesc(EstadoPublicacion.PENDIENTE).size();
    }

    /* ---------------------- Validación del formulario ---------------------- */

    private void aplicar(Alojamiento a, AlojamientoForm f) {
        String titulo = f.getTitulo() == null ? "" : f.getTitulo().trim();
        if (titulo.isEmpty() || titulo.length() > 150) throw new ReglaNegocioException("El título es obligatorio (máx. 150 caracteres).");
        TipoAlojamiento tipo;
        try {
            tipo = TipoAlojamiento.valueOf(f.getTipo());
        } catch (Exception e) {
            throw new ReglaNegocioException("Tipo de alojamiento inválido.");
        }
        if (f.getPrecioMensual() == null || f.getPrecioMensual().signum() < 0) throw new ReglaNegocioException("El precio debe ser 0 o mayor.");
        if (f.getPrecioMensual().compareTo(new BigDecimal("999999")) > 0) throw new ReglaNegocioException("El precio es demasiado alto.");
        String direccion = f.getDireccion() == null ? "" : f.getDireccion().trim();
        String distrito = f.getDistrito() == null ? "" : f.getDistrito().trim();
        if (direccion.isEmpty() || distrito.isEmpty()) throw new ReglaNegocioException("La dirección y el distrito son obligatorios.");
        Double lat = f.getLatitud(), lng = f.getLongitud();
        if (lat == null || lng == null)
            throw new ReglaNegocioException("Marca la ubicación exacta en el mapa (o busca la dirección) antes de guardar.");
        if (lat < LAT_MIN || lat > LAT_MAX || lng < LNG_MIN || lng > LNG_MAX)
            throw new ReglaNegocioException("La ubicación marcada está fuera de Lima y Callao. Revisa el punto en el mapa.");
        int capacidad = f.getCapacidad() == null ? 1 : f.getCapacidad();
        if (capacidad < 1 || capacidad > 20) throw new ReglaNegocioException("La capacidad debe estar entre 1 y 20.");

        a.setTitulo(titulo);
        a.setDescripcion(f.getDescripcion() == null ? "" : f.getDescripcion().trim());
        a.setTipo(tipo);
        a.setPrecioMensual(f.getPrecioMensual());
        a.setDireccion(direccion);
        a.setDistrito(distrito);
        a.setLatitud(lat);
        a.setLongitud(lng);
        a.setCapacidad(capacidad);
        a.setAmoblado(f.isAmoblado());
        a.setWifi(f.isWifi());
        a.setAdmiteRoommates(f.isAdmiteRoommates());
        a.setAdmiteMascotas(f.isAdmiteMascotas());
    }

    private static List<String> urlsDe(String texto) {
        List<String> urls = new ArrayList<>();
        if (texto == null) return urls;
        for (String linea : texto.split("\\R")) {
            String u = linea.trim();
            if ((u.startsWith("http://") || u.startsWith("https://")) && u.length() <= 500 && urls.size() < 10) urls.add(u);
        }
        return urls;
    }
}
