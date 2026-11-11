package br.com.fiap.sobraboaapi.notificacao;

import br.com.fiap.sobraboaapi.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificações")
@SecurityRequirement(name = "bearerAuth")
public class NotificacaoController {
    private final NotificacaoService service;

    @GetMapping("/minhas")
    @Operation(summary = "Avisos gerados pelos eventos assíncronos (RabbitMQ), do mais novo para o mais antigo")
    public Page<NotificacaoResponse> minhas(@ParameterObject Pageable p, @AuthenticationPrincipal UsuarioLogado u) {
        return service.minhas(u.id(), p);
    }

    @PatchMapping("/{id}/lida")
    @Operation(summary = "Marca uma notificação como lida")
    public ResponseEntity<Void> marcarLida(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado u) {
        service.marcarLida(id, u.id());
        return ResponseEntity.noContent().build();
    }
}