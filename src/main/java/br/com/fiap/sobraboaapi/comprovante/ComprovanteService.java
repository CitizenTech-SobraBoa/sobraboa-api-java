package br.com.fiap.sobraboaapi.comprovante;

import br.com.fiap.sobraboaapi.exception.AcessoNegadoException;
import br.com.fiap.sobraboaapi.exception.RecursoNaoEncontradoException;
import br.com.fiap.sobraboaapi.reserva.Reserva;
import br.com.fiap.sobraboaapi.reserva.ReservaRepository;
import br.com.fiap.sobraboaapi.security.UsuarioLogado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComprovanteService {

    private final ComprovanteRepository comprovanteRepo;
    private final ReservaRepository reservaRepo;

    /** Só vê o comprovante: admin, o receptor da reserva ou o doador da doação. */
    @Transactional(readOnly = true)
    public ComprovanteResponse buscarPorReserva(Long reservaId, UsuarioLogado u) {
        Reserva r = reservaRepo.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        boolean permitido = "ADMIN".equals(u.papel())
                || r.getReceptor().getUsuarioId().equals(u.id())
                || r.getDoacao().getDoador().getUsuarioId().equals(u.id());


        if (!permitido) throw new AcessoNegadoException("Você não participa desta reserva");

        Comprovante c = comprovanteRepo.findByReservaId(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comprovante ainda não emitido"));

        return new ComprovanteResponse(c.getCodigoVerificacao(), c.getPesoTotalKg(), c.getEmitidoEm(),
                r.getDoacao().getId(), r.getDoacao().getTitulo(),
                r.getDoacao().getDoador().getNomeFantasia(), r.getReceptor().getNomeOrganizacao(),
                c.getUrlPdf());
    }
}