package br.com.fiap.sobraboaapi.doacaoitem;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ItemRequest(
        @NotNull Short categoriaId,
        @NotBlank @Size(max = 200) String descricao,
        @NotNull @Positive BigDecimal quantidade,
        @NotNull UnidadeMedida unidade,
        @PositiveOrZero BigDecimal pesoEstimadoKg,
        LocalDate dataValidade,
        OffsetDateTime preparadoEm) {
}
