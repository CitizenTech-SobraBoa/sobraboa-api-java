package br.com.fiap.sobraboaapi.security;

import br.com.fiap.sobraboaapi.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Valida o JWT e, a cada requisição, confere no banco se o usuário continua ativo e não foi excluído.
 * O papel usado é o do BANCO (não o do token), então mudança de papel/desativação vale na hora.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.validar(header.substring(7))
                    .flatMap(token -> usuarioRepo.findById(token.id()))
                    .filter(u -> u.isAtivo() && u.getDeletadoEm() == null)
                    .ifPresent(u -> {
                        var logado = new UsuarioLogado(u.getId(), u.getEmail(), u.getPapelPrincipal().name());
                        var auth = new UsernamePasswordAuthenticationToken(
                                logado, null, List.of(new SimpleGrantedAuthority("ROLE_" + logado.papel())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        chain.doFilter(req, res);
    }
}