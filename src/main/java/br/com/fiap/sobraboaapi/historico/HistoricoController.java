package br.com.fiap.sobraboaapi.historico;

import br.com.fiap.sobraboaapi.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doacoes")
@RequiredArgsConstructor
@Tag(name = "Doações")
@SecurityRequirement(name = "bearerAuth")
public class HistoricoController {

    private final HistoricoService service;

    @GetMapping("/{id}/historico")
    @Operation(summary = "Linha do tempo (mudanças de status) de uma doação")
    public List<HistoricoResponse> historico(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado u) {
        return service.linhaDoTempo(id, u);
    }
}