package com.unihousing.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idReporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reportante", nullable = false)
    private Usuario reportante;

    @ManyToOne(fetch = FetchType.LAZY, optional=true)
    @JoinColumn(name = "id_usuario_reportado")
    private Usuario usuarioReportado;

    @ManyToOne(fetch = FetchType.LAZY, optional=true)
    @JoinColumn(name = "id_alojamiento_reportado")
    private Alojamiento alojamientoReportado;

    private String motivo;

    @Enumerated(EnumType.STRING)
    private EstadoReporte estado;

    private LocalDateTime fecha;

    public Reporte() {}

    public Integer getIdReporte() { return idReporte; }
    public void setIdReporte(Integer idReporte) { this.idReporte = idReporte; }

    public Usuario getReportante() { return reportante; }
    public void setReportante(Usuario reportante) { this.reportante = reportante; }

    public Usuario getUsuarioReportado() { return usuarioReportado; }
    public void setUsuarioReportado(Usuario usuarioReportado) { this.usuarioReportado = usuarioReportado; }

    public Alojamiento getAlojamientoReportado() { return alojamientoReportado; }
    public void setAlojamientoReportado(Alojamiento alojamientoReportado) { this.alojamientoReportado = alojamientoReportado; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public EstadoReporte getEstado() { return estado; }
    public void setEstado(EstadoReporte estado) { this.estado = estado; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) fecha = LocalDateTime.now();
        if (estado == null) estado = EstadoReporte.PENDIENTE;
    }
}
