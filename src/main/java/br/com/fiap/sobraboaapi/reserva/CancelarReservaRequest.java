package br.com.fiap.sobraboaapi.reserva;

import jakarta.validation.constraints.Size;

public record CancelarReservaRequest(@Size(max = 300) String motivo) {
}