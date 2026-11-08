package br.com.fiap.sobraboaapi.ia;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record AssistenteDoacaoRequest(
        @NotBlank @Size(min = 10, max = 1500) String texto,
        @NotNull OffsetDateTime janelaInicio,
        @NotNull @Future OffsetDateTime janelaFim) {
}