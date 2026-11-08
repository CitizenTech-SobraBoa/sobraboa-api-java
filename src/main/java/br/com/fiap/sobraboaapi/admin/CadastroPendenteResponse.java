package br.com.fiap.sobraboaapi.admin;

import br.com.fiap.sobraboaapi.shared.StatusAprovacao;

public record CadastroPendenteResponse(Long usuarioId, String nome, String email, String cnpj, StatusAprovacao status) {
}