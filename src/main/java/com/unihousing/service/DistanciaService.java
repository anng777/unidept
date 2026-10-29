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
 * Distancia y tiempo caminando entre un alojamiento y una universidad.
 *  - Con GOOGLE_MAPS_API_KEY: Google Distance Matrix (ruta real a pie).
 *  - Sin clave: distancia en línea recta (Haversine) x 1.3 (factor de calles) y 80 m/min (4,8 km/h).
 * Siempre se calcula con las MISMAS coordenadas que se guardan y que abre el enlace de Google Maps.
 */
@Service
public class DistanciaService {
    public static final double FACTOR_CALLES = 1.3;
    public static final double METROS_POR_MINUTO = 80.0;

    public record ResultadoDistancia(double metros, int minutos, boolean rutaReal) {}

    private final String claveGoogle;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public DistanciaService(@Value("${google.maps.key:}") String claveGoogle) {
        this.claveGoogle = claveGoogle == null ? "" : claveGoogle.trim();
    }

    public ResultadoDistancia calcular(double lat1, double lng1, double lat2, double lng2) {
        if (!claveGoogle.isEmpty()) {
            ResultadoDistancia g = viaGoogle(lat1, lng1, lat2, lng2);
            if (g != null) return g;
        }
        double metros = Math.round(haversine(lat1, lng1, lat2, lng2) * FACTOR_CALLES);
        int minutos = (int) Math.max(1, Math.round(metros / METROS_POR_MINUTO));
        return new ResultadoDistancia(metros, minutos, false);
    }

    /** Distancia en línea recta, en metros. */
    public static double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double r = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private ResultadoDistancia viaGoogle(double lat1, double lng1, double lat2, double lng2) {
        try {
            String url = "https://maps.googleapis.com/maps/api/distancematrix/json?mode=walking&origins=" + lat1 + "," + lng1
                    + "&destinations=" + lat2 + "," + lng2 + "&key=" + claveGoogle;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> resp = cliente.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            JsonNode el = mapper.readTree(resp.body()).path("rows").path(0).path("elements").path(0);
            if (!"OK".equals(el.path("status").asText())) return null;
            double metros = el.path("distance").path("value").asDouble();
            int minutos = (int) Math.max(1, Math.round(el.path("duration").path("value").asDouble() / 60.0));
            return new ResultadoDistancia(metros, minutos, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
