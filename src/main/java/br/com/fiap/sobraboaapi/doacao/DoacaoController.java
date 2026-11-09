package br.com.fiap.sobraboaapi.doacao;

import br.com.fiap.sobraboaapi.ia.AssistenteDoacaoRequest;
import br.com.fiap.sobraboaapi.ia.AssistenteDoacaoService;
import br.com.fiap.sobraboaapi.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doacoes")
@RequiredArgsConstructor
@Tag(name = "Doações")
@SecurityRequirement(name = "bearerAuth")
public class DoacaoController {
    private final DoacaoService service;
    private final AssistenteDoacaoService assistente;

    @PostMapping
    public ResponseEntity<DoacaoResponse> criar(@RequestBody @Valid DoacaoRequest r,
                                                @AuthenticationPrincipal UsuarioLogado u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(r, u.id()));
    }

    @PostMapping("/assistente")
    @Operation(summary = "Cria um RASCUNHO de doação a partir de texto livre usando IA")
    public ResponseEntity<DoacaoResponse> assistente(@RequestBody @Valid AssistenteDoacaoRequest r,
                                                     @AuthenticationPrincipal UsuarioLogado u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assistente.criarRascunho(r, u.id()));
    }

    @GetMapping("/{id}")
    public DoacaoResponse buscar(@PathVariable Long id) { return service.buscar(id); }

    @GetMapping("/minhas")
    public Page<DoacaoResponse> minhas(@ParameterObject Pageable p, @AuthenticationPrincipal UsuarioLogado u) {
        return service.minhas(u.id(), p);
    }

    @GetMapping("/proximas")
    public Page<DoacaoResponse> proximas(@RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
                                         @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
                                         @RequestParam(defaultValue = "5") @Positive @Max(50) double raioKm,
                                         @RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return service.proximas(lat, lng, raioKm, PageRequest.of(page, size));
    }

    @PutMapping("/{id}")
    public DoacaoResponse atualizar(@PathVariable Long id, @RequestBody @Valid DoacaoRequest r,
                                    @AuthenticationPrincipal UsuarioLogado u) {
        return service.atualizar(id, r, u.id());
    }

    @PatchMapping("/{id}/publicar")
    public DoacaoResponse publicar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado u) {
        return service.publicar(id, u.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado u) {
        service.cancelar(id, u.id());
        return ResponseEntity.noContent().build();
    }
}