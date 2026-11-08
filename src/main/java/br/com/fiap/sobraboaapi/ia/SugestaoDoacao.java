package br.com.fiap.sobraboaapi.ia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SugestaoDoacao(
        String titulo,
        String descricao,
        boolean requerRefrigeracao,
        String instrucoesRetirada,
        List<ItemSugerido> itens) {

    public record ItemSugerido(
            String descricao,
            String categoria,
            BigDecimal quantidade,
            String unidade,
            BigDecimal pesoEstimadoKg,
            LocalDate dataValidade) {}
}
