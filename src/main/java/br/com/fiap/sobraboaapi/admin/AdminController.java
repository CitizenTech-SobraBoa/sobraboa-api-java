package br.com.fiap.sobraboaapi.admin;

import br.com.fiap.sobraboaapi.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final AdminService admin;

    @GetMapping("/doadores/pendentes")
    public Page<CadastroPendenteResponse> doadores(@ParameterObject Pageable p) {
        return admin.doadoresPendentes(p);
    }

    @GetMapping("/receptores/pendentes")
    public Page<CadastroPendenteResponse> receptores(@ParameterObject Pageable p) {
        return admin.receptoresPendentes(p);
    }

    @PatchMapping("/doadores/{id}/aprovacao")
    public ResponseEntity<Void> decidirDoador(@PathVariable Long id, @RequestBody @Valid DecisaoCadastroRequest r,
                                              @AuthenticationPrincipal UsuarioLogado admin) {
        this.admin.decidirDoador(id, r.aprovado(), admin.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/receptores/{id}/aprovacao")
    public ResponseEntity<Void> decidirReceptor(@PathVariable Long id, @RequestBody @Valid DecisaoCadastroRequest r,
                                                @AuthenticationPrincipal UsuarioLogado admin) {
        this.admin.decidirReceptor(id, r.aprovado(), admin.id());
        return ResponseEntity.noContent().build();
    }
}
