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

    @Bean
    TopicExchange exchange() { return new TopicExchange(EXCHANGE); }
    @Bean
    Queue filaNotificacoes() { return QueueBuilder.durable(FILA_NOTIFICACOES).build(); }
    @Bean
    Binding binding(Queue filaNotificacoes, TopicExchange exchange) {
        return BindingBuilder.bind(filaNotificacoes).to(exchange).with(ROUTING);
    }

    @Bean
    MessageConverter jsonConverter() {
        return new JacksonJsonMessageConverter();
    }
}