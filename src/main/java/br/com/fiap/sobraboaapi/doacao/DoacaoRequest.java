package br.com.fiap.sobraboaapi.doacao;

import br.com.fiap.sobraboaapi.doacaoitem.ItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.List;

public record DoacaoRequest(
        @NotBlank @Size(max = 150) String titulo,
        String descricao,
        @NotNull @Future OffsetDateTime janelaFim,
        @NotNull OffsetDateTime janelaInicio,
        boolean requerRefrigeracao,
        @Size(max = 500) String instrucoesRetirada,
        @Size(max = 500) String fotoUrl,
        Long enderecoRetiradaId,
        @NotEmpty List<@Valid ItemRequest> itens) {
}