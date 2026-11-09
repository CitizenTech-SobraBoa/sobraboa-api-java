package br.com.fiap.sobraboaapi.admin;

import br.com.fiap.sobraboaapi.usuario.PapelUsuario;
import br.com.fiap.sobraboaapi.usuario.Usuario;
import br.com.fiap.sobraboaapi.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.senha}")
    private String senha;

    @Override
    public void run(String... args) {
        if (repo.existsByEmail(email)) return;
        Usuario u = new Usuario();
        u.setNome("Administrador SobraBoa");
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(senha));
        u.setPapelPrincipal(PapelUsuario.ADMIN);
        u.setAceitouTermosEm(OffsetDateTime.now());
        repo.save(u);
        log.info("Admin inicial criado: {}", email);
    }
}
