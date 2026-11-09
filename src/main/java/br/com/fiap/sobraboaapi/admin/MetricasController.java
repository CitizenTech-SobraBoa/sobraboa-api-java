package br.com.fiap.sobraboaapi.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/metricas")
@RequiredArgsConstructor
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class MetricasController {

    private final MetricasService service;

    @GetMapping
    @Operation(summary = "Indicadores de impacto: doações por status, kg doados, taxa de aproveitamento")
    public MetricasResponse metricas() {
        return service.calcular();
    }
}