package com.unihousing.service;

/** Error de regla de negocio con un mensaje listo para mostrar al usuario. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
