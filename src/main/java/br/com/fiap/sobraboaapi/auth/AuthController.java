package br.com.fiap.sobraboaapi.auth;

import br.com.fiap.sobraboaapi.doador.RegistroDoadorRequest;
import br.com.fiap.sobraboaapi.receptor.RegistroReceptorRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {
    private final AuthService auth;

    @PostMapping("/registro/doador")
    public ResponseEntity<Map<String, Object>> registrarDoador(@RequestBody @Valid RegistroDoadorRequest r) {
        Long id = auth.registrarDoador(r);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("usuarioId", id, "mensagem", "Cadastro enviado. Aguarde a aprovação do administrador."));
    }

    @PostMapping("/registro/receptor")
    public ResponseEntity<Map<String, Object>> registrarReceptor(@RequestBody @Valid RegistroReceptorRequest r) {
        Long id = auth.registrarReceptor(r);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("usuarioId", id, "mensagem", "Cadastro enviado. Aguarde a aprovação do administrador."));
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest r) { return auth.login(r); }
}