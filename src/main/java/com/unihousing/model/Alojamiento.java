package com.unihousing.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alojamientos")
public class Alojamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAlojamiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_arrendador", nullable = false)
    private Usuario arrendador;

    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private TipoAlojamiento tipo;

    private BigDecimal precioMensual;

    private String direccion;

    private String distrito;

    private double latitud;

    private double longitud;

    private Integer capacidad;

    private boolean amoblado;

    private boolean wifi;

    private boolean admiteRoommates;

    private boolean admiteMascotas;

    private boolean disponible = true;

    @Enumerated(EnumType.STRING)
    private EstadoPublicacion estadoPublicacion;

    private LocalDateTime fechaPublicacion;

    @OneToMany(mappedBy = "alojamiento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idImagen ASC")
    private List<ImagenAlojamiento> imagenes = new ArrayList<>();

    @Transient
    private String universidadReferencia;

    @Transient
    private Double distanciaMetros;

    @Transient
    private Integer tiempoMinutos;

    @Transient
    private boolean ocupadoAhora;

    public Alojamiento() {}

    public Integer getIdAlojamiento() { return idAlojamiento; }
    public void setIdAlojamiento(Integer idAlojamiento) { this.idAlojamiento = idAlojamiento; }

    public Usuario getArrendador() { return arrendador; }
    public void setArrendador(Usuario arrendador) { this.arrendador = arrendador; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public TipoAlojamiento getTipo() { return tipo; }
    public void setTipo(TipoAlojamiento tipo) { this.tipo = tipo; }

    public BigDecimal getPrecioMensual() { return precioMensual; }
    public void setPrecioMensual(BigDecimal precioMensual) { this.precioMensual = precioMensual; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public boolean isAmoblado() { return amoblado; }
    public void setAmoblado(boolean amoblado) { this.amoblado = amoblado; }

    public boolean isWifi() { return wifi; }
    public void setWifi(boolean wifi) { this.wifi = wifi; }

    public boolean isAdmiteRoommates() { return admiteRoommates; }
    public void setAdmiteRoommates(boolean admiteRoommates) { this.admiteRoommates = admiteRoommates; }

    public boolean isAdmiteMascotas() { return admiteMascotas; }
    public void setAdmiteMascotas(boolean admiteMascotas) { this.admiteMascotas = admiteMascotas; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public EstadoPublicacion getEstadoPublicacion() { return estadoPublicacion; }
    public void setEstadoPublicacion(EstadoPublicacion estadoPublicacion) { this.estadoPublicacion = estadoPublicacion; }

    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public void setFechaPublicacion(LocalDateTime fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }

    public List<ImagenAlojamiento> getImagenes() { return imagenes; }
    public void setImagenes(List<ImagenAlojamiento> imagenes) { this.imagenes = imagenes; }

    public String getUniversidadReferencia() { return universidadReferencia; }
    public void setUniversidadReferencia(String universidadReferencia) { this.universidadReferencia = universidadReferencia; }

    public Double getDistanciaMetros() { return distanciaMetros; }
    public void setDistanciaMetros(Double distanciaMetros) { this.distanciaMetros = distanciaMetros; }

    public Integer getTiempoMinutos() { return tiempoMinutos; }
    public void setTiempoMinutos(Integer tiempoMinutos) { this.tiempoMinutos = tiempoMinutos; }

    public boolean isOcupadoAhora() { return ocupadoAhora; }
    public void setOcupadoAhora(boolean ocupadoAhora) { this.ocupadoAhora = ocupadoAhora; }

    @PrePersist
    void antesDeGuardar() {
        if (fechaPublicacion == null) fechaPublicacion = LocalDateTime.now();
    }

    public String getImagenPrincipal() {
        return imagenes.isEmpty() ? "https://picsum.photos/seed/alojamiento" + idAlojamiento + "-1/700/500" : imagenes.get(0).getUrlImagen();
    }

    public List<String> getUrlsImagenes() {
        List<String> urls = new ArrayList<>();
        for (ImagenAlojamiento i : imagenes) urls.add(i.getUrlImagen());
        if (urls.isEmpty()) {
            for (int n = 1; n <= 3; n++) urls.add("https://picsum.photos/seed/alojamiento" + idAlojamiento + "-" + n + "/700/500");
        }
        return urls;
    }

    public void reemplazarImagenes(List<String> urls) {
        imagenes.clear();
        for (String u : urls) {
            ImagenAlojamiento i = new ImagenAlojamiento();
            i.setAlojamiento(this);
            i.setUrlImagen(u);
            imagenes.add(i);
        }
    }

    /** Enlace a Google Maps que abre EXACTAMENTE el punto guardado (latitud, longitud). */
    public String getMapsUrl() {
        return "https://www.google.com/maps/search/?api=1&query=" + latitud + "," + longitud;
    }
}
