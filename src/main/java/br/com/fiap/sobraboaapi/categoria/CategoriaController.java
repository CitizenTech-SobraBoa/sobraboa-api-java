package br.com.fiap.sobraboaapi.categoria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorias")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {
    private final CategoriaAlimentoRepository repo;

    @GetMapping
    @Operation(summary = "Lista as categorias de alimento (o app precisa dos ids para montar os itens da doação)")
    public List<CategoriaResponse> listar() {
        return repo.findAll(Sort.by("nome")).stream().map(CategoriaResponse::de).toList();
    }
}