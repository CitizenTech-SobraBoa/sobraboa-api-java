package br.com.fiap.sobraboaapi.security;

import br.com.fiap.sobraboaapi.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiracaoMin;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expiracaoMin) {

        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret ausente ou fraco: defina JWT_SECRET com no mínimo 32 caracteres "
                            + "(ex.: openssl rand -base64 48).");
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMin = expiracaoMin;
    }

    public String gerar(Usuario u) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(u.getEmail())
                .claim("uid", u.getId())
                .claim("papel", u.getPapelPrincipal().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMin, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public OffsetDateTime expiraEm() {
        return OffsetDateTime.now().plusMinutes(expiracaoMin);
    }

    public Optional<UsuarioLogado> validar(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new UsuarioLogado(c.get("uid", Long.class), c.getSubject(), c.get("papel", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}