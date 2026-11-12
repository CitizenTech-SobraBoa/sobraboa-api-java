package br.com.fiap.sobraboaapi.doador;

import br.com.fiap.sobraboaapi.endereco.EnderecoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RegistroDoadorRequest(
        @Schema(example = "Carlos Pereira")
        @NotBlank @Size(max = 150) String nome,

        @Schema(example = "padaria@exemplo.com")
        @NotBlank @Email String email,

        @Schema(example = "11999998888")
        String telefone,

        @Schema(example = "Senha@123")
        @NotBlank @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres") String senha,

        @Schema(example = "Padaria Pão Quente")
        @NotBlank String nomeFantasia,

        @Schema(example = "Pão Quente Alimentos LTDA")
        String razaoSocial,

        @Schema(example = "11.222.333/0001-81")
        @Pattern(regexp = "^\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}$", message = "CNPJ inválido") String cnpj,

        @Schema(example = "Padaria")
        String tipoEstabelecimento,

        @NotNull @Valid EnderecoRequest endereco,

        @Schema(example = "true")
        @AssertTrue(message = "É necessário aceitar os termos de responsabilidade") boolean aceitouTermos) {
}