package br.com.fiap.sobraboaapi.historico;

import br.com.fiap.sobraboaapi.doacao.Doacao;
import br.com.fiap.sobraboaapi.doacao.DoacaoRepository;
import br.com.fiap.sobraboaapi.exception.AcessoNegadoException;
import br.com.fiap.sobraboaapi.exception.RecursoNaoEncontradoException;
import br.com.fiap.sobraboaapi.reserva.ReservaRepository;
import br.com.fiap.sobraboaapi.security.UsuarioLogado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoricoService {
    private final DoacaoRepository doacaoRepo;
    private final ReservaRepository reservaRepo;
    private final DoacaoStatusHistoricoRepository historicoRepo;

    /**
     * Linha do tempo da doação. Só vê: admin, o doador dono ou um receptor que reservou a doação.
     */
    @Transactional(readOnly = true)
    public List<HistoricoResponse> linhaDoTempo(Long doacaoId, UsuarioLogado u) {

        Doacao d = doacaoRepo.findById(doacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doação não encontrada"));

        boolean permitido = "ADMIN".equals(u.papel())
                || d.getDoador().getUsuarioId().equals(u.id())
                || reservaRepo.existsByDoacaoIdAndReceptorUsuarioId(doacaoId, u.id());

        if (!permitido) throw new AcessoNegadoException("Você não participa desta doação");

        return historicoRepo.findByDoacaoIdOrderByAlteradoEmAsc(doacaoId).stream()
                .map(HistoricoResponse::de).toList();
    }
}