package com.unihousing.service;

public class AlojamientoForm {
    private String titulo;
    private String descripcion;
    private String tipo = "HABITACION";
    private java.math.BigDecimal precioMensual;
    private String direccion;
    private String distrito;
    private Double latitud;
    private Double longitud;
    private Integer capacidad = 1;
    private boolean amoblado;
    private boolean wifi;
    private boolean admiteRoommates;
    private boolean admiteMascotas;
    private boolean disponible = true;
    private String imagenes;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public java.math.BigDecimal getPrecioMensual() { return precioMensual; }
    public void setPrecioMensual(java.math.BigDecimal precioMensual) { this.precioMensual = precioMensual; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

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

    public String getImagenes() { return imagenes; }
    public void setImagenes(String imagenes) { this.imagenes = imagenes; }
}
