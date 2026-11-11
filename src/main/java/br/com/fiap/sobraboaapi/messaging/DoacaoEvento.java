package br.com.fiap.sobraboaapi.messaging;

import java.time.Instant;
import java.util.UUID;

public record DoacaoEvento(
        UUID eventoId,
        String tipo,
        Long doacaoId,
        Long reservaId,
        Long doadorId,
        Long receptorId,
        String titulo,
        Instant ocorridoEm) {
}
