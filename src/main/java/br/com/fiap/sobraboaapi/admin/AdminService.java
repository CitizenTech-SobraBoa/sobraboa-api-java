package br.com.fiap.sobraboaapi.admin;

import br.com.fiap.sobraboaapi.doador.Doador;
import br.com.fiap.sobraboaapi.doador.DoadorRepository;
import br.com.fiap.sobraboaapi.exception.RecursoNaoEncontradoException;
import br.com.fiap.sobraboaapi.receptor.Receptor;
import br.com.fiap.sobraboaapi.receptor.ReceptorRepository;
import br.com.fiap.sobraboaapi.shared.StatusAprovacao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final DoadorRepository doadorRepo;
    private final ReceptorRepository receptorRepo;

    @Transactional(readOnly = true)
    public Page<CadastroPendenteResponse> doadoresPendentes(Pageable p) {
        return doadorRepo.findByStatusAprovacao(StatusAprovacao.PENDENTE, p)
                .map(d -> new CadastroPendenteResponse(d.getUsuarioId(), d.getNomeFantasia(),
                        d.getUsuario().getEmail(), d.getCnpj(), d.getStatusAprovacao()));
    }

    @Transactional(readOnly = true)
    public Page<CadastroPendenteResponse> receptoresPendentes(Pageable p) {
        return receptorRepo.findByStatusAprovacao(StatusAprovacao.PENDENTE, p)
                .map(r -> new CadastroPendenteResponse(r.getUsuarioId(), r.getNomeOrganizacao(),
                        r.getUsuario().getEmail(), r.getCnpj(), r.getStatusAprovacao()));
    }

    @Transactional
    public void decidirDoador(Long id, boolean aprovado, Long adminId) {
        Doador d = doadorRepo.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Doador não encontrado"));
        d.setStatusAprovacao(aprovado ? StatusAprovacao.APROVADO : StatusAprovacao.REJEITADO);
        d.setAprovadoPor(adminId);
        d.setAprovadoEm(OffsetDateTime.now());
    }

    @Transactional
    public void decidirReceptor(Long id, boolean aprovado, Long adminId) {
        Receptor r = receptorRepo.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Receptor não encontrado"));
        r.setStatusAprovacao(aprovado ? StatusAprovacao.APROVADO : StatusAprovacao.REJEITADO);
        r.setAprovadoPor(adminId);
        r.setAprovadoEm(OffsetDateTime.now());
    }
}