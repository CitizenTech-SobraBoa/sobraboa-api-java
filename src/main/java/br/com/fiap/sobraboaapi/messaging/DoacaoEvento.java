package br.com.fiap.sobraboaapi.messaging;

import java.time.Instant;

public record DoacaoEvento(
        String tipo,
        Long doacaoId,
        Long reservaId,
        Long doadorId,
        Long receptorId,
        String titulo,
        Instant ocorridoEm) {
}
