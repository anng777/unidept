package com.unihousing.config;

import com.unihousing.security.JwtAuthFilter;
import com.unihousing.security.JwtService;
import com.unihousing.security.OrigenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;

/**
 * Spring Security SIN sesión (STATELESS): la identidad viaja en un JWT.
 * Roles reales: ESTUDIANTE, ARRENDADOR y ADMINISTRADOR.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter, JwtService jwtService) throws Exception {
        http
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Protección CSRF: la cookie del JWT es SameSite=Lax (el navegador no la envía en POST de otros sitios)
            // y además OrigenFilter rechaza POST cuyo Origin/Referer no sea este mismo sitio.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/error", "/error/**").permitAll()
                .requestMatchers("/", "/login", "/registro", "/admin/login", "/api/auth/**").permitAll()
                // Rutas por rol (primero las restringidas, luego las públicas)
                .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/mis-alojamientos/**", "/alojamientos/nuevo", "/alojamientos/guardar",
                        "/alojamientos/*/editar", "/alojamientos/*/disponibilidad", "/alojamientos/*/disponibilidad/**")
                        .hasRole("ARRENDADOR")
                .requestMatchers("/solicitudes/crear", "/mis-solicitudes").hasRole("ESTUDIANTE")
                .requestMatchers(HttpMethod.GET, "/alojamientos", "/alojamientos/{id:\\d+}", "/api/dni",
                        "/api/alojamientos/*/ocupadas").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    String uri = req.getRequestURI();
                    if (uri.contains("/api/")) {
                        res.sendError(401, "No autenticado");
                    } else {
                        res.sendRedirect(req.getContextPath() + (uri.contains("/admin") ? "/admin/login" : "/login"));
                    }
                })
                .accessDeniedHandler((req, res, ex) -> res.sendError(403)))
            .logout(l -> l
                .logoutUrl("/logout")
                .addLogoutHandler((req, res, auth) ->
                        res.addHeader(HttpHeaders.SET_COOKIE, jwtService.cookieBorrada().toString()))
                .logoutSuccessUrl("/"))
            .addFilterBefore(new OrigenFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
