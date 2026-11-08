package br.com.fiap.sobraboaapi.doador;

import br.com.fiap.sobraboaapi.endereco.EnderecoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RegistroDoadorRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email String email,
        String telefone,
        @NotBlank @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres") String senha,
        @NotBlank String nomeFantasia,
        String razaoSocial,
        @Pattern(regexp = "^\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}$", message = "CNPJ inválido") String cnpj,
        String tipoEstabelecimento,
        @NotNull @Valid EnderecoRequest endereco,
        @AssertTrue(message = "É necessário aceitar os termos de responsabilidade") boolean aceitouTermos) {}