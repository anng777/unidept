package com.unihousing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Verifica un DNI contra RENIEC mediante la API de Decolecta
 * (https://api.decolecta.com/v1/reniec/dni?numero=...). Requiere el token en RENIEC_API_TOKEN.
 */
@Service
public class ReniecService {
    private static final String ENDPOINT = "https://api.decolecta.com/v1/reniec/dni?numero=";

    public record ResultadoDni(boolean encontrado, String nombres, String apellidoPaterno, String apellidoMaterno, String mensajeError) {
        public String apellidos() {
            return ((apellidoPaterno == null ? "" : apellidoPaterno) + " " + (apellidoMaterno == null ? "" : apellidoMaterno)).trim();
        }
    }

    private final String token;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public ReniecService(@Value("${reniec.token:}") String token) {
        this.token = token == null ? "" : token.trim();
    }

    public ResultadoDni consultar(String dni) {
        if (token.isEmpty()) {
            return error("El servicio RENIEC no está configurado (falta la variable de entorno RENIEC_API_TOKEN).");
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(ENDPOINT + dni))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .GET().build();
            HttpResponse<String> resp = cliente.send(req, HttpResponse.BodyHandlers.ofString());
            int codigo = resp.statusCode();
            if (codigo == 200) {
                JsonNode j = mapper.readTree(resp.body());
                String nombres = primero(j, "first_name", "nombres");
                String paterno = primero(j, "first_last_name", "apellido_paterno", "apellidoPaterno");
                String materno = primero(j, "second_last_name", "apellido_materno", "apellidoMaterno");
                if (nombres == null || nombres.isBlank()) return error("RENIEC no devolvió datos para este DNI.");
                return new ResultadoDni(true, nombres, paterno, materno, null);
            }
            if (codigo == 401 || codigo == 403) return error("Token de RENIEC inválido o sin permisos.");
            if (codigo == 400 || codigo == 404 || codigo == 422) return error("DNI no encontrado en RENIEC.");
            if (codigo == 429) return error("Se alcanzó el límite de consultas de RENIEC. Intenta más tarde.");
            return error("RENIEC respondió con error (" + codigo + ").");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("La consulta a RENIEC fue interrumpida.");
        } catch (Exception e) {
            return error("No se pudo conectar con RENIEC: " + e.getMessage());
        }
    }

    private static ResultadoDni error(String mensaje) {
        return new ResultadoDni(false, null, null, null, mensaje);
    }

    private static String primero(JsonNode j, String... claves) {
        for (String c : claves) {
            JsonNode n = j.get(c);
            if (n != null && !n.isNull() && !n.asText().isBlank()) return n.asText().trim();
        }
        return null;
    }
}
