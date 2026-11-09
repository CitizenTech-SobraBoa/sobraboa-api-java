package br.com.fiap.sobraboaapi.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificacaoConsumer {

    @RabbitListener(queues = RabbitConfig.FILA_NOTIFICACOES)
    public void receber(DoacaoEvento e) {
        // No MVP o "aviso" é um log. Evolua para e-mail (JavaMailSender) ou push para o app Mobile.
        switch (e.tipo()) {
            case "DOACAO_RESERVADA" ->
                    log.info("[AVISO] Doador {}: sua doação '{}' foi reservada pelo receptor {}.", e.doadorId(), e.titulo(), e.receptorId());
            case "RETIRADA_CONFIRMADA" ->
                    log.info("[AVISO] Receptor {}: retirada de '{}' confirmada.", e.receptorId(), e.titulo());
            case "DOACAO_CONCLUIDA" ->
                    log.info("[AVISO] Doação '{}' concluída. Comprovante disponível.", e.titulo());
            case "RESERVA_CANCELADA" ->
                    log.info("[AVISO] Doador {}: a reserva de '{}' foi cancelada; a doação voltou a ficar disponível.", e.doadorId(), e.titulo());
            default -> log.warn("Evento desconhecido: {}", e.tipo());
        }
    }
}