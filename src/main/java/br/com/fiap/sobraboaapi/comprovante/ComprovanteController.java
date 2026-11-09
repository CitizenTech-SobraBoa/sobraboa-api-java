package br.com.fiap.sobraboaapi.comprovante;

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

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas")
@SecurityRequirement(name = "bearerAuth")
public class ComprovanteController {
    private final ComprovanteService service;

    @GetMapping("/{id}/comprovante")
    @Operation(summary = "Comprovante de doação (emitido quando o receptor confirma o recebimento)")
    public ComprovanteResponse comprovante(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado u) {
        return service.buscarPorReserva(id, u);
    }
}