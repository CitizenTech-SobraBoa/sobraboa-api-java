package br.com.fiap.sobraboaapi.historico;

import br.com.fiap.sobraboaapi.doacao.StatusDoacao;

import java.time.OffsetDateTime;

public record HistoricoResponse(StatusDoacao statusAnterior, StatusDoacao statusNovo,
                                Long alteradoPor, OffsetDateTime alteradoEm, String observacao) {
    public static HistoricoResponse de(DoacaoStatusHistorico h) {
        return new HistoricoResponse(h.getStatusAnterior(), h.getStatusNovo(),
                h.getAlteradoPor(), h.getAlteradoEm(), h.getObservacao());
    }
}