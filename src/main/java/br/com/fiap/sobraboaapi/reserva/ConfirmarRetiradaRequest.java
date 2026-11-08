package br.com.fiap.sobraboaapi.reserva;

import jakarta.validation.constraints.NotBlank;

public record ConfirmarRetiradaRequest(@NotBlank String codigo) {
}
