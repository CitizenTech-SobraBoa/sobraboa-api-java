package br.com.fiap.sobraboaapi.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventoPublisherTest {

    private DoacaoEvento evento() {
        return new DoacaoEvento(UUID.randomUUID(), "DOACAO_RESERVADA", 1L, 2L, 3L, 4L, "Pães", Instant.now());
    }

    @Test
    void publicaNaExchangeComARoutingKeyDoTipo() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        DoacaoEvento e = evento();

        new EventoPublisher(rabbit).aoOcorrer(e);

        verify(rabbit).convertAndSend(eq(RabbitConfig.EXCHANGE), eq("doacao.doacao_reservada"), eq(e));
    }

    @Test
    void brokerForaDoArNaoDerrubaARequisicao() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doThrow(new AmqpConnectException(new RuntimeException("broker fora")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> new EventoPublisher(rabbit).aoOcorrer(evento()));
    }
}