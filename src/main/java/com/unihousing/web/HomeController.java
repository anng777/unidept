package com.unihousing.web;

import com.unihousing.model.Alojamiento;
import com.unihousing.service.AlojamientoService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final AlojamientoService alojamientos;

    public HomeController(AlojamientoService alojamientos) {
        this.alojamientos = alojamientos;
    }

    @GetMapping("/")
    public String index(Model m) {
        List<Alojamiento> todos = alojamientos.buscar(null, null, null, null, false, false, false);
        List<Alojamiento> libres = todos.stream().filter(a -> !a.isOcupadoAhora()).toList();
        m.addAttribute("destacados", libres.stream().limit(6).toList());
        m.addAttribute("totalAlojamientos", todos.size());
        m.addAttribute("totalDisponibles", libres.size());
        m.addAttribute("universidades", alojamientos.universidades());
        return "index";
    }
}
