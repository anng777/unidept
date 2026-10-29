package com.unihousing.security;

import com.unihousing.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/** Genera y valida los JWT (firmados con HMAC-SHA256). */
@Service
public class JwtService {
    private final SecretKey clave;
    private final Duration vigencia;
    private final String nombreCookie;

    public JwtService(@Value("${unidept.jwt.secret}") String secreto,
                      @Value("${unidept.jwt.expiracion-horas}") long horas,
                      @Value("${unidept.jwt.cookie}") String nombreCookie) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("unidept.jwt.secret debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.vigencia = Duration.ofHours(horas);
        this.nombreCookie = nombreCookie;
    }

    public String getNombreCookie() { return nombreCookie; }

    public String generar(Usuario u) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(u.getIdUsuario()))
                .claim("rol", u.getTipoUsuario().name())
                .claim("usuario", u.getNombreUsuario())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + vigencia.toMillis()))
                .signWith(clave)
                .compact();
    }

    /** Devuelve el id de usuario si el token es válido y no expiró. */
    public Optional<Integer> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return Optional.of(Integer.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Cookie HttpOnly que lleva el JWT en las páginas (el navegador la envía sola). */
    public ResponseCookie crearCookie(String token) {
        return ResponseCookie.from(nombreCookie, token)
                .httpOnly(true).sameSite("Lax").path("/").maxAge(vigencia).build();
    }

    /** Cookie vencida con los MISMOS atributos (path "/") para que el navegador sí la borre al salir. */
    public ResponseCookie cookieBorrada() {
        return ResponseCookie.from(nombreCookie, "")
                .httpOnly(true).sameSite("Lax").path("/").maxAge(0).build();
    }
}
