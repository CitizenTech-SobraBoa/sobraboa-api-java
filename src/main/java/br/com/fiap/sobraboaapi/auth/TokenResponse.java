package br.com.fiap.sobraboaapi.auth;

import br.com.fiap.sobraboaapi.shared.StatusAprovacao;
import br.com.fiap.sobraboaapi.usuario.PapelUsuario;

import java.time.OffsetDateTime;

public record TokenResponse(String token, String tipo, OffsetDateTime expiraEm,
                            Long usuarioId, String nome, PapelUsuario papel, StatusAprovacao aprovacao) {
}
