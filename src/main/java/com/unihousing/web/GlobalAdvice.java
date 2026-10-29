package com.unihousing.web;

import com.unihousing.model.Usuario;
import com.unihousing.security.SesionUtil;
import com.unihousing.service.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

@ControllerAdvice
public class GlobalAdvice {
    @Value("${google.maps.key:}")
    private String mapsKey;

    /** Disponible en todas las plantillas como ${yo}. */
    @ModelAttribute("yo")
    public Usuario yo() {
        return SesionUtil.actual();
    }

    @ModelAttribute("tieneMapsKey")
    public boolean tieneMapsKey() {
        return mapsKey != null && !mapsKey.isBlank();
    }

    /** Errores de regla de negocio: vuelven a la página anterior con un mensaje (o JSON 400 en /api/). */
    @ExceptionHandler(ReglaNegocioException.class)
    public ModelAndView reglaNegocio(ReglaNegocioException e, HttpServletRequest req, HttpServletResponse res) {
        if (req.getRequestURI().contains("/api/")) {
            MappingJackson2JsonView json = new MappingJackson2JsonView();
            res.setStatus(400);
            return new ModelAndView(json, java.util.Map.of("error", e.getMessage()));
        }
        FlashMap flash = RequestContextUtils.getOutputFlashMap(req);
        flash.put("error", e.getMessage());
        String ref = req.getHeader("Referer");
        String destino = ref != null && ref.startsWith(req.getScheme() + "://" + req.getServerName()) ? ref : req.getContextPath() + "/";
        return new ModelAndView(new RedirectView(destino, false));
    }
}
