package br.com.fiap.sobraboaapi.security;

import br.com.fiap.sobraboaapi.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper mapper;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/doacoes/proximas", "/api/reservas/*/receber",
                                "/api/reservas/minhas", "/api/doacoes/*/reservas").hasRole("RECEPTOR")
                        .requestMatchers(HttpMethod.POST, "/api/doacoes", "/api/doacoes/assistente").hasRole("DOADOR")
                        .requestMatchers("/api/doacoes/minhas", "/api/reservas/*/retirada").hasRole("DOADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/doacoes/*").hasRole("DOADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/doacoes/*/publicar").hasRole("DOADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/doacoes/*").hasRole("DOADOR")
                        .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health/**").permitAll()
                        .requestMatchers("/api/doacoes/proximas", "/api/reservas/*/receber",
                                "/api/reservas/*/cancelar", "/api/reservas/minhas", "/api/doacoes/*/reservas").hasRole("RECEPTOR")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> erro(res, 401, "Token ausente ou inválido", req))
                        .accessDeniedHandler((req, res, ex) -> erro(res, 403, "Você não tem permissão para esta operação", req)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void erro(HttpServletResponse res, int status, String msg, HttpServletRequest req) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(),
                new ApiError(Instant.now(), status, HttpStatus.valueOf(status).getReasonPhrase(), msg, req.getRequestURI(), null));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}