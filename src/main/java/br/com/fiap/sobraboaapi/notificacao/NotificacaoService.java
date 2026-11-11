package br.com.fiap.sobraboaapi.notificacao;

import br.com.fiap.sobraboaapi.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificacaoService {
    private final NotificacaoRepository repo;

    @Transactional(readOnly = true)
    public Page<NotificacaoResponse> minhas(Long usuarioId, Pageable p) {
        return repo.findByUsuarioIdOrderByCriadaEmDesc(usuarioId, p).map(NotificacaoResponse::de);
    }

    @Transactional
    public void marcarLida(Long id, Long usuarioId) {
        Notificacao n = repo.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada"));
        n.setLida(true);   // a entidade é gerenciada: o UPDATE sai no commit
    }
}