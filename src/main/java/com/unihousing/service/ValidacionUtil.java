package com.unihousing.service;

import java.util.Set;
import java.util.regex.Pattern;

public final class ValidacionUtil {
    private static final Pattern CELULAR = Pattern.compile("^9\\d{8}$");
    private static final Pattern USUARIO = Pattern.compile("^[a-zA-Z0-9_.]{4,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern DNI = Pattern.compile("^\\d{8}$");
    private static final Set<String> DOMINIOS_PERSONALES = Set.of(
            "gmail.com", "hotmail.com", "outlook.com", "yahoo.com", "yahoo.es", "live.com", "icloud.com", "msn.com", "protonmail.com");

    private ValidacionUtil() {}

    public static boolean esCelularValido(String s) { return s != null && CELULAR.matcher(s).matches(); }
    public static boolean esNombreUsuarioValido(String s) { return s != null && USUARIO.matcher(s).matches(); }
    public static boolean esDniValido(String s) { return s != null && DNI.matcher(s).matches(); }
    public static boolean esEmailValido(String s) { return s != null && EMAIL.matcher(s).matches(); }

    /** Correo institucional: termina en .edu.pe o .edu y no es de un dominio personal. */
    public static boolean esCorreoInstitucional(String email) {
        if (!esEmailValido(email)) return false;
        String dominio = email.toLowerCase().substring(email.indexOf('@') + 1);
        if (DOMINIOS_PERSONALES.contains(dominio)) return false;
        return dominio.endsWith(".edu.pe") || dominio.endsWith(".edu");
    }
}
