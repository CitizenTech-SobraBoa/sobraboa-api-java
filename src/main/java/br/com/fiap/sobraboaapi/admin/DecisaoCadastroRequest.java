package br.com.fiap.sobraboaapi.admin;

import jakarta.validation.constraints.NotNull;

public record DecisaoCadastroRequest(@NotNull Boolean aprovado) {}

