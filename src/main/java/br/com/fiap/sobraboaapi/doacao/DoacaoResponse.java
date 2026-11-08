package br.com.fiap.sobraboaapi.doacao;

import br.com.fiap.sobraboaapi.doacaoitem.UnidadeMedida;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DoacaoResponse(
        Long id, String titulo, String descricao, StatusDoacao status,
        OffsetDateTime janelaInicio, OffsetDateTime janelaFim,
        boolean requerRefrigeracao, String instrucoesRetirada, String fotoUrl,
        String doador, String cidade, List<ItemResponse> itens) {

    public record ItemResponse(Long id, String categoria, String descricao, BigDecimal quantidade,
                               UnidadeMedida unidade, BigDecimal pesoEstimadoKg, LocalDate dataValidade) {}

    public static DoacaoResponse de(Doacao d) {
        var itens = d.getItens().stream().map(i -> new ItemResponse(
                i.getId(), i.getCategoria().getNome(), i.getDescricao(), i.getQuantidade(),
                i.getUnidade(), i.getPesoEstimadoKg(), i.getDataValidade())).toList();
        return new DoacaoResponse(d.getId(), d.getTitulo(), d.getDescricao(), d.getStatus(),
                d.getJanelaInicio(), d.getJanelaFim(), d.isRequerRefrigeracao(),
                d.getInstrucoesRetirada(), d.getFotoUrl(),
                d.getDoador().getNomeFantasia(), d.getEnderecoRetirada().getCidade(), itens);
    }
}
