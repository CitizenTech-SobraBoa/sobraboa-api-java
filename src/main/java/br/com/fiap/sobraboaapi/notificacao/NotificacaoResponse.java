package br.com.fiap.sobraboaapi.notificacao;

import java.time.OffsetDateTime;

public record NotificacaoResponse(Long id, String tipo, String mensagem, boolean lida, OffsetDateTime criadaEm) {
    public static NotificacaoResponse de(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getTipo(), n.getMensagem(), n.isLida(), n.getCriadaEm());
    }
}