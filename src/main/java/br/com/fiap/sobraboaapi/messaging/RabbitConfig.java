package br.com.fiap.sobraboaapi.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "sobraboa.eventos";
    public static final String FILA_NOTIFICACOES = "sobraboa.notificacoes";
    public static final String ROUTING = "doacao.#";

    public static final String DLX = "sobraboa.eventos.dlx";
    public static final String FILA_DLQ = "sobraboa.notificacoes.dlq";

    @Bean
    TopicExchange exchange() { return new TopicExchange(EXCHANGE); }

    @Bean
    Queue filaNotificacoes() {
        return QueueBuilder.durable(FILA_NOTIFICACOES)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(FILA_DLQ)
                .build();
    }

    @Bean
    Binding binding(Queue filaNotificacoes, TopicExchange exchange) {
        return BindingBuilder.bind(filaNotificacoes).to(exchange).with(ROUTING);
    }

    @Bean
    DirectExchange dlx() {
        return new DirectExchange(DLX);
    }

    @Bean
    Queue filaDlq() {
        return QueueBuilder.durable(FILA_DLQ).build();
    }

    @Bean
    Binding bindingDlq() {
        return BindingBuilder.bind(filaDlq()).to(dlx()).with(FILA_DLQ);
    }

    @Bean
    MessageConverter jsonConverter() {
        return new JacksonJsonMessageConverter();
    }
}