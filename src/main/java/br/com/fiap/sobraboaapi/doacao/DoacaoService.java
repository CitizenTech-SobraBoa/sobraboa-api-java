package br.com.fiap.sobraboaapi.doacao;

import br.com.fiap.sobraboaapi.categoria.CategoriaAlimentoRepository;
import br.com.fiap.sobraboaapi.doacaoitem.DoacaoItem;
import br.com.fiap.sobraboaapi.doacaoitem.ItemRequest;
import br.com.fiap.sobraboaapi.doador.Doador;
import br.com.fiap.sobraboaapi.doador.DoadorRepository;
import br.com.fiap.sobraboaapi.endereco.EnderecoRepository;
import br.com.fiap.sobraboaapi.exception.AcessoNegadoException;
import br.com.fiap.sobraboaapi.exception.RecursoNaoEncontradoException;
import br.com.fiap.sobraboaapi.exception.RegraNegocioException;
import br.com.fiap.sobraboaapi.historico.DoacaoStatusHistorico;
import br.com.fiap.sobraboaapi.historico.DoacaoStatusHistoricoRepository;
import br.com.fiap.sobraboaapi.shared.StatusAprovacao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DoacaoService {
    private final DoacaoRepository doacaoRepo;
    private final DoadorRepository doadorRepo;
    private final EnderecoRepository enderecoRepo;
    private final CategoriaAlimentoRepository categoriaRepo;
    private final DoacaoStatusHistoricoRepository historicoRepo;

    @Transactional
    public DoacaoResponse criar(DoacaoRequest r, Long doadorId) {
        Doador doador = doadorAprovado(doadorId);
        Doacao d = new Doacao();
        preencher(d, r, doador);
        d = doacaoRepo.save(d);
        registrarHistorico(d.getId(), null, StatusDoacao.RASCUNHO, doadorId, "Rascunho criado");
        return DoacaoResponse.de(d);
    }

    @Transactional(readOnly = true)
    public DoacaoResponse buscar(Long id) {
        return DoacaoResponse.de(doacaoRepo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doação não encontrada")));
    }

    @Transactional(readOnly = true)
    public Page<DoacaoResponse> minhas(Long doadorId, Pageable p) {
        return doacaoRepo.findByDoadorUsuarioId(doadorId, p).map(DoacaoResponse::de);
    }

    @Transactional
    public DoacaoResponse atualizar(Long id, DoacaoRequest r, Long doadorId) {
        Doacao d = doacaoDoDoador(id, doadorId);
        if (d.getStatus() != StatusDoacao.RASCUNHO && d.getStatus() != StatusDoacao.DISPONIVEL)
            throw new RegraNegocioException("Só é possível editar doações em rascunho ou disponíveis");
        preencher(d, r, d.getDoador());
        d.setAtualizadoEm(OffsetDateTime.now());
        return DoacaoResponse.de(d);
    }

    @Transactional
    public DoacaoResponse publicar(Long id, Long doadorId) {
        Doacao d = doacaoDoDoador(id, doadorId);
        if (d.getStatus() != StatusDoacao.RASCUNHO)
            throw new RegraNegocioException("Apenas rascunhos podem ser publicados");
        if (d.getJanelaFim().isBefore(OffsetDateTime.now()))
            throw new RegraNegocioException("A janela de retirada já terminou");
        mudarStatus(d, StatusDoacao.DISPONIVEL, doadorId, "Doação publicada");
        return DoacaoResponse.de(d);
    }

    @Transactional
    public void cancelar(Long id, Long doadorId) {
        Doacao d = doacaoDoDoador(id, doadorId);
        if (d.getStatus() == StatusDoacao.RETIRADA || d.getStatus() == StatusDoacao.CONCLUIDA
                || d.getStatus() == StatusDoacao.EXPIRADA || d.getStatus() == StatusDoacao.CANCELADA)
            throw new RegraNegocioException("Esta doação não pode mais ser cancelada");
        mudarStatus(d, StatusDoacao.CANCELADA, doadorId, "Cancelada pelo doador");
    }

    @Transactional(readOnly = true)
    public Page<DoacaoResponse> proximas(double lat, double lng, double raioKm, Pageable p) {
        double dLat = raioKm / 111.32;
        double dLng = raioKm / (111.32 * Math.cos(Math.toRadians(lat)));
        Pageable semSort = PageRequest.of(p.getPageNumber(), p.getPageSize());
        return doacaoRepo.buscarProximas(lat, lng, raioKm,
                lat - dLat, lat + dLat, lng - dLng, lng + dLng, semSort).map(DoacaoResponse::de);
    }


    public void mudarStatus(Doacao d, StatusDoacao novo, Long usuarioId, String obs) {
        StatusDoacao anterior = d.getStatus();
        d.setStatus(novo);
        d.setAtualizadoEm(OffsetDateTime.now());
        registrarHistorico(d.getId(), anterior, novo, usuarioId, obs);
    }

    private void registrarHistorico(Long doacaoId, StatusDoacao ant, StatusDoacao novo, Long por, String obs) {
        DoacaoStatusHistorico h = new DoacaoStatusHistorico();
        h.setDoacaoId(doacaoId);
        h.setStatusAnterior(ant);
        h.setStatusNovo(novo);
        h.setAlteradoPor(por);
        h.setObservacao(obs);
        historicoRepo.save(h);
    }

    private Doador doadorAprovado(Long id) {
        Doador doador = doadorRepo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doador não encontrado"));
        if (doador.getStatusAprovacao() != StatusAprovacao.APROVADO)
            throw new AcessoNegadoException("Seu cadastro ainda não foi aprovado pelo administrador");
        return doador;
    }

    private Doacao doacaoDoDoador(Long id, Long doadorId) {
        Doacao d = doacaoRepo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doação não encontrada"));
        if (!d.getDoador().getUsuarioId().equals(doadorId))
            throw new AcessoNegadoException("Esta doação pertence a outro doador");
        return d;
    }

    private void preencher(Doacao d, DoacaoRequest r, Doador doador) {
        if (!r.janelaFim().isAfter(r.janelaInicio()))
            throw new RegraNegocioException("A janela de retirada deve terminar depois de começar");
        d.setDoador(doador);
        d.setTitulo(r.titulo());
        d.setDescricao(r.descricao());
        d.setJanelaInicio(r.janelaInicio());
        d.setJanelaFim(r.janelaFim());
        d.setRequerRefrigeracao(r.requerRefrigeracao());
        d.setInstrucoesRetirada(r.instrucoesRetirada());
        d.setFotoUrl(r.fotoUrl());
        d.setEnderecoRetirada(r.enderecoRetiradaId() == null ? doador.getEndereco()
                : enderecoRepo.findById(r.enderecoRetiradaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado")));
        d.getItens().clear();
        for (ItemRequest i : r.itens()) {
            DoacaoItem item = new DoacaoItem();
            item.setDoacao(d);
            item.setCategoria(categoriaRepo.findById(i.categoriaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + i.categoriaId() + " não existe")));
            item.setDescricao(i.descricao());
            item.setQuantidade(i.quantidade());
            item.setUnidade(i.unidade());
            item.setPesoEstimadoKg(i.pesoEstimadoKg());
            item.setDataValidade(i.dataValidade());
            item.setPreparadoEm(i.preparadoEm());
            d.getItens().add(item);
        }
    }
}
