package br.com.fiap.sobraboaapi.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventoPublisher {
    private final RabbitTemplate rabbit;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoOcorrer(DoacaoEvento e) {
        String routingKey = "doacao." + e.tipo().toLowerCase();
        rabbit.convertAndSend(RabbitConfig.EXCHANGE, routingKey, e);
        log.info("Evento publicado: {} (doação {})", e.tipo(), e.doacaoId());
    }
}
