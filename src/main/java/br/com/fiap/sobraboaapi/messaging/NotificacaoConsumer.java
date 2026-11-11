package br.com.fiap.sobraboaapi.messaging;

import br.com.fiap.sobraboaapi.notificacao.Notificacao;
import br.com.fiap.sobraboaapi.notificacao.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacaoConsumer {

    private final NotificacaoRepository repo;

    @RabbitListener(queues = RabbitConfig.FILA_NOTIFICACOES)
    @Transactional
    public void receber(DoacaoEvento e) {
        switch (e.tipo()) {
            case "DOACAO_RESERVADA" -> salvar(e, e.doadorId(),
                    "Sua doação '%s' foi reservada.".formatted(e.titulo()));
            case "RETIRADA_CONFIRMADA" -> salvar(e, e.receptorId(),
                    "A retirada de '%s' foi confirmada.".formatted(e.titulo()));
            case "DOACAO_CONCLUIDA" -> {
                salvar(e, e.doadorId(), "A doação '%s' foi concluída.".formatted(e.titulo()));
                salvar(e, e.receptorId(), "O recebimento de '%s' foi confirmado.".formatted(e.titulo()));
            }
            case "RESERVA_CANCELADA" -> salvar(e, e.doadorId(),
                    "A reserva de '%s' foi cancelada; a doação voltou a ficar disponível.".formatted(e.titulo()));
            default -> log.warn("Evento desconhecido: {}", e.tipo());
        }
    }

    private void salvar(DoacaoEvento e, Long usuarioId, String mensagem) {
        if (repo.existsByEventoIdAndUsuarioId(e.eventoId(), usuarioId)) return;   // mensagem repetida: ignora
        Notificacao n = new Notificacao();
        n.setEventoId(e.eventoId());
        n.setUsuarioId(usuarioId);
        n.setTipo(e.tipo());
        n.setMensagem(mensagem);
        repo.save(n);
        log.info("[AVISO] usuário {}: {}", usuarioId, mensagem);
    }
}