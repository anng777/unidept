package com.unihousing.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ocupaciones")
public class Ocupacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idOcupacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_alojamiento", nullable = false)
    private Alojamiento alojamiento;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    private String nota;

    @ManyToOne(fetch = FetchType.LAZY, optional=true)
    @JoinColumn(name = "id_solicitud")
    private Solicitud solicitud;

    public Ocupacion() {}

    public Integer getIdOcupacion() { return idOcupacion; }
    public void setIdOcupacion(Integer idOcupacion) { this.idOcupacion = idOcupacion; }

    public Alojamiento getAlojamiento() { return alojamiento; }
    public void setAlojamiento(Alojamiento alojamiento) { this.alojamiento = alojamiento; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }

    public Solicitud getSolicitud() { return solicitud; }
    public void setSolicitud(Solicitud solicitud) { this.solicitud = solicitud; }
}
