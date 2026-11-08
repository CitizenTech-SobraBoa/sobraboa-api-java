package br.com.fiap.sobraboaapi.reserva;

import java.time.OffsetDateTime;

public record ReservaResponse(Long id, Long doacaoId, String tituloDoacao, StatusReserva status,
                              String codigoRetirada, OffsetDateTime expiraEm) {
    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(r.getId(), r.getDoacao().getId(), r.getDoacao().getTitulo(),
                r.getStatus(), r.getCodigoRetirada(), r.getExpiraEm());
    }
}