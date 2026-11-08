package br.com.fiap.sobraboaapi.endereco;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record EnderecoRequest(
        @NotBlank @Size(max = 200) String logradouro,
        @Size(max = 20) String numero,
        @Size(max = 100) String complemento,
        @Size(max = 100) String bairro,
        @NotBlank @Size(max = 100) String cidade,
        @NotBlank @Size(min = 2, max = 2) String uf,
        @Size(max = 9) String cep,
        @NotNull @DecimalMin("-90")  @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {}

