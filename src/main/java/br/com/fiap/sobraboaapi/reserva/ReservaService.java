package br.com.fiap.sobraboaapi.reserva;

import br.com.fiap.sobraboaapi.comprovante.Comprovante;
import br.com.fiap.sobraboaapi.comprovante.ComprovanteRepository;
import br.com.fiap.sobraboaapi.doacao.Doacao;
import br.com.fiap.sobraboaapi.doacao.DoacaoRepository;
import br.com.fiap.sobraboaapi.doacao.DoacaoService;
import br.com.fiap.sobraboaapi.doacao.StatusDoacao;
import br.com.fiap.sobraboaapi.doacaoitem.DoacaoItem;
import br.com.fiap.sobraboaapi.exception.AcessoNegadoException;
import br.com.fiap.sobraboaapi.exception.ConflitoException;
import br.com.fiap.sobraboaapi.exception.RecursoNaoEncontradoException;
import br.com.fiap.sobraboaapi.exception.RegraNegocioException;
import br.com.fiap.sobraboaapi.messaging.DoacaoEvento;
import br.com.fiap.sobraboaapi.receptor.Receptor;
import br.com.fiap.sobraboaapi.receptor.ReceptorRepository;
import br.com.fiap.sobraboaapi.shared.StatusAprovacao;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservaService {
    private final DoacaoRepository doacaoRepo;
    private final ReservaRepository reservaRepo;
    private final ReceptorRepository receptorRepo;
    private final ComprovanteRepository comprovanteRepo;
    private final DoacaoService doacaoService;
    private final ApplicationEventPublisher eventos;

    @Value("${app.reserva.minutos-para-retirar}") private long minutosParaRetirar;
    private static final SecureRandom RND = new SecureRandom();

    /** Consulta 5.2: trava a linha da doação, cria a reserva e muda o status. */
    @Transactional
    public ReservaResponse reservar(Long doacaoId, Long receptorId) {
        Receptor receptor = receptorRepo.findById(receptorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Receptor não encontrado"));
        if (receptor.getStatusAprovacao() != StatusAprovacao.APROVADO)
            throw new AcessoNegadoException("Seu cadastro ainda não foi aprovado pelo administrador");

        Doacao d = doacaoRepo.findByIdForUpdate(doacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doação não encontrada"));

        OffsetDateTime agora = OffsetDateTime.now();
        if (d.getStatus() != StatusDoacao.DISPONIVEL)
            throw new ConflitoException("Doação já reservada ou indisponível");
        if (agora.isBefore(d.getJanelaInicio()) || agora.isAfter(d.getJanelaFim()))
            throw new RegraNegocioException("Fora da janela de retirada");
        if (d.isRequerRefrigeracao() && !receptor.isPossuiRefrigeracao())
            throw new RegraNegocioException("Esta doação exige refrigeração e sua organização não possui");

        Reserva r = new Reserva();
        r.setDoacao(d);
        r.setReceptor(receptor);
        r.setCodigoRetirada(gerarCodigo());
        OffsetDateTime expira = agora.plusMinutes(minutosParaRetirar);
        r.setExpiraEm(expira.isAfter(d.getJanelaFim()) ? d.getJanelaFim() : expira);

        try {
            r = reservaRepo.saveAndFlush(r);   // o índice único parcial do banco é a última barreira
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException("Doação já reservada");
        }

        doacaoService.mudarStatus(d, StatusDoacao.RESERVADA, receptorId, "Reservada pelo receptor");
        publicar("DOACAO_RESERVADA", r);
        return ReservaResponse.de(r);
    }

    /** Consulta 5.3: o DOADOR informa o código mostrado ao receptor. */
    @Transactional
    public ReservaResponse confirmarRetirada(Long reservaId, String codigo, Long doadorId) {
        Reserva r = reservaRepo.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));
        if (!r.getDoacao().getDoador().getUsuarioId().equals(doadorId))
            throw new AcessoNegadoException("Esta reserva não é de uma doação sua");
        if (r.getStatus() != StatusReserva.ATIVA)
            throw new RegraNegocioException("Reserva não está ativa");
        if (!r.getCodigoRetirada().equalsIgnoreCase(codigo.trim()))
            throw new RegraNegocioException("Código de retirada inválido");

        r.setStatus(StatusReserva.RETIRADA);
        r.setRetiradaEm(OffsetDateTime.now());
        doacaoService.mudarStatus(r.getDoacao(), StatusDoacao.RETIRADA, doadorId, "Retirada confirmada");
        publicar("RETIRADA_CONFIRMADA", r);
        return ReservaResponse.de(r);
    }

    /** Consulta 5.4: o RECEPTOR confirma o recebimento e o comprovante é emitido. */
    @Transactional
    public ReservaResponse confirmarRecebimento(Long reservaId, Long receptorId) {
        Reserva r = reservaRepo.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));
        if (!r.getReceptor().getUsuarioId().equals(receptorId))
            throw new AcessoNegadoException("Esta reserva pertence a outro receptor");
        if (r.getStatus() != StatusReserva.RETIRADA)
            throw new RegraNegocioException("A retirada ainda não foi confirmada pelo doador");

        r.setStatus(StatusReserva.CONCLUIDA);
        r.setRecebidaEm(OffsetDateTime.now());
        doacaoService.mudarStatus(r.getDoacao(), StatusDoacao.CONCLUIDA, receptorId, "Recebimento confirmado");

        BigDecimal peso = r.getDoacao().getItens().stream()
                .map(DoacaoItem::getPesoEstimadoKg)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Comprovante c = new Comprovante();
        c.setReservaId(r.getId());
        c.setPesoTotalKg(peso);
        c.setCodigoVerificacao("SB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        comprovanteRepo.save(c);

        publicar("DOACAO_CONCLUIDA", r);
        return ReservaResponse.de(r);
    }

    /** O RECEPTOR desiste da reserva ativa: a doação volta a ficar DISPONIVEL (se a janela ainda estiver aberta). */
    @Transactional
    public ReservaResponse cancelar(Long reservaId, String motivo, Long receptorId) {
        Reserva r = reservaRepo.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (!r.getReceptor().getUsuarioId().equals(receptorId))
            throw new AcessoNegadoException("Esta reserva pertence a outro receptor");
        if (r.getStatus() != StatusReserva.ATIVA)
            throw new RegraNegocioException("Só é possível cancelar reservas ativas");

        Doacao d = doacaoRepo.findByIdForUpdate(r.getDoacao().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doação não encontrada"));
        r.setStatus(StatusReserva.CANCELADA);
        r.setMotivoCancelamento(motivo);

        StatusDoacao novoStatus = d.getJanelaFim().isAfter(OffsetDateTime.now())
                ? StatusDoacao.DISPONIVEL : StatusDoacao.EXPIRADA;
        doacaoService.mudarStatus(d, novoStatus, receptorId, "Reserva cancelada pelo receptor");
        publicar("RESERVA_CANCELADA", r);

        return ReservaResponse.de(r);
    }

    @Transactional(readOnly = true)
    public Page<ReservaResponse> minhas(Long receptorId, Pageable p) {
        return reservaRepo.findByReceptorUsuarioId(receptorId, p).map(ReservaResponse::de);
    }

    private void publicar(String tipo, Reserva r) {
        eventos.publishEvent(new DoacaoEvento(UUID.randomUUID(), tipo, r.getDoacao().getId(), r.getId(),
                r.getDoacao().getDoador().getUsuarioId(), r.getReceptor().getUsuarioId(),
                r.getDoacao().getTitulo(), Instant.now()));
    }

    private String gerarCodigo() {
        String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";   // sem caracteres ambíguos (0/O, 1/I)
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(alfabeto.charAt(RND.nextInt(alfabeto.length())));
        return sb.toString();
    }
}
