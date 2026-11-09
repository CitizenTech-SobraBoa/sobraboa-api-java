package br.com.fiap.sobraboaapi.reserva;

import br.com.fiap.sobraboaapi.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Reservas")
@SecurityRequirement(name = "bearerAuth")
public class ReservaController {
    private final ReservaService service;

    @PostMapping("/doacoes/{id}/reservas")
    public ResponseEntity<ReservaResponse> reservar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reservar(id, u.id()));
    }

    @PostMapping("/reservas/{id}/retirada")
    public ReservaResponse retirada(@PathVariable Long id, @RequestBody @Valid ConfirmarRetiradaRequest r,
                                    @AuthenticationPrincipal UsuarioLogado u) {
        return service.confirmarRetirada(id, r.codigo(), u.id());
    }

    @PostMapping("/reservas/{id}/receber")
    public ReservaResponse receber(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado u) {
        return service.confirmarRecebimento(id, u.id());
    }

    @PostMapping("/reservas/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id,
                                    @RequestBody(required = false) @Valid CancelarReservaRequest r,
                                    @AuthenticationPrincipal UsuarioLogado u) {
        return service.cancelar(id, r == null ? null : r.motivo(), u.id());
    }

    @GetMapping("/reservas/minhas")
    public Page<ReservaResponse> minhas(@ParameterObject Pageable p, @AuthenticationPrincipal UsuarioLogado u) {
        return service.minhas(u.id(), p);
    }
}
