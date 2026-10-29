package com.unihousing.web;

import com.unihousing.model.TipoUsuario;
import com.unihousing.model.Usuario;
import com.unihousing.security.JwtService;
import com.unihousing.security.UsuarioPrincipal;
import com.unihousing.service.RegistroForm;
import com.unihousing.service.UsuarioService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UsuarioService usuarios;

    public AuthController(AuthenticationManager authManager, JwtService jwt, UsuarioService usuarios) {
        this.authManager = authManager;
        this.jwt = jwt;
        this.usuarios = usuarios;
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/admin/login")
    public String adminLogin() { return "admin-login"; }

    @GetMapping("/registro")
    public String registro(Model m) {
        if (!m.containsAttribute("form")) m.addAttribute("form", new RegistroForm());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@ModelAttribute("form") RegistroForm form, RedirectAttributes ra) {
        try {
            usuarios.registrar(form);
        } catch (com.unihousing.service.ReglaNegocioException e) {
            form.setPassword(null);
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
            return "redirect:/registro";
        }
        ra.addFlashAttribute("ok", "Cuenta creada y verificada. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    /** Login con formulario (correo o usuario). Entrega el JWT en una cookie HttpOnly. */
    @PostMapping("/login")
    public String loginForm(@RequestParam String identificador, @RequestParam String password,
                            HttpServletResponse resp, RedirectAttributes ra) {
        return iniciar(identificador, password, false, resp, ra);
    }

    @PostMapping("/admin/login")
    public String adminLoginForm(@RequestParam String identificador, @RequestParam String password,
                                 HttpServletResponse resp, RedirectAttributes ra) {
        return iniciar(identificador, password, true, resp, ra);
    }

    private String iniciar(String id, String pass, boolean admin, HttpServletResponse resp, RedirectAttributes ra) {
        String volver = admin ? "redirect:/admin/login" : "redirect:/login";
        try {
            Usuario u = autenticar(id, pass);
            boolean esAdmin = u.getTipoUsuario() == TipoUsuario.ADMINISTRADOR;
            if (admin != esAdmin) {
                ra.addFlashAttribute("error", admin ? "Esta cuenta no es de administrador." : "Los administradores ingresan por /admin/login.");
                return volver;
            }
            resp.addHeader(HttpHeaders.SET_COOKIE, jwt.crearCookie(jwt.generar(u)).toString());
            return esAdmin ? "redirect:/admin" : "redirect:/alojamientos";
        } catch (DisabledException e) {
            ra.addFlashAttribute("error", "Tu cuenta está suspendida. Contacta al administrador.");
        } catch (AuthenticationException e) {
            ra.addFlashAttribute("error", "Usuario/correo o contraseña incorrectos.");
        }
        return volver;
    }

    /** API: devuelve el JWT en JSON (para usarlo como Authorization: Bearer). */
    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<?> loginApi(@RequestBody Map<String, String> body) {
        try {
            Usuario u = autenticar(body.getOrDefault("identificador", ""), body.getOrDefault("password", ""));
            return ResponseEntity.ok(Map.of("token", jwt.generar(u), "tipoUsuario", u.getTipoUsuario().name()));
        } catch (DisabledException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Cuenta suspendida"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    private Usuario autenticar(String id, String pass) {
        if (id == null || id.isBlank() || pass == null) throw new BadCredentialsException("vacío");
        Authentication a = authManager.authenticate(new UsernamePasswordAuthenticationToken(id.trim(), pass));
        return ((UsuarioPrincipal) a.getPrincipal()).getUsuario();
    }
}
