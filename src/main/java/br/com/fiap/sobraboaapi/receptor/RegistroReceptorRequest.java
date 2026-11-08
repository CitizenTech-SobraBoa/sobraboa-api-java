package br.com.fiap.sobraboaapi.receptor;

import br.com.fiap.sobraboaapi.endereco.EnderecoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RegistroReceptorRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email String email,
        String telefone,
        @NotBlank @Size(min = 8) String senha,
        @NotBlank String nomeOrganizacao,
        @Pattern(regexp = "^\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}$", message = "CNPJ inválido") String cnpj,
        String tipo,
        @PositiveOrZero Integer pessoasAtendidas,
        boolean possuiRefrigeracao,
        String horarioFuncionamento,
        @NotNull @Valid EnderecoRequest endereco,
        @AssertTrue(message = "É necessário aceitar os termos de responsabilidade") boolean aceitouTermos) {
}
