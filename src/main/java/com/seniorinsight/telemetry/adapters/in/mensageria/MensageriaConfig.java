package com.seniorinsight.telemetry.adapters.in.mensageria;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fila {@code telemetry.q}, ligada à exchange {@code si.events} pelos eventos que o serviço consome, com
 * dead-letter: mensagem rejeitada vai para {@code telemetry.q.dlq} pela exchange {@code telemetry.dlx}.
 * Tudo é declarado (idempotente) na conexão.
 */
@Configuration(proxyBeanMethods = false)
public class MensageriaConfig {

    public static final String FILA = "telemetry.q";
    public static final String FILA_DEAD_LETTER = "telemetry.q.dlq";
    static final String EXCHANGE_EVENTOS = "si.events";
    static final String EXCHANGE_DEAD_LETTER = "telemetry.dlx";
    static final String FABRICA_LISTENER = "fabricaListenerTelemetria";

    /** Routing keys assinadas: consentimento (si-telemetry-service#3) e LGPD (si-telemetry-service#4). */
    static final List<String> EVENTOS_ASSINADOS = List.of("consent.changed", "lgpd.erasure.requested");

    @Bean
    Declarables filaTelemetria() {
        TopicExchange eventos = ExchangeBuilder.topicExchange(EXCHANGE_EVENTOS).durable(true).build();
        DirectExchange deadLetter = ExchangeBuilder.directExchange(EXCHANGE_DEAD_LETTER).durable(true).build();
        Queue fila = QueueBuilder.durable(FILA)
                .deadLetterExchange(EXCHANGE_DEAD_LETTER)
                .deadLetterRoutingKey(FILA)
                .build();
        Queue filaDeadLetter = QueueBuilder.durable(FILA_DEAD_LETTER).build();

        List<Declarable> declaraveis = new ArrayList<>(List.of(eventos, deadLetter, fila, filaDeadLetter,
                BindingBuilder.bind(filaDeadLetter).to(deadLetter).with(FILA)));
        for (String routingKey : EVENTOS_ASSINADOS) {
            declaraveis.add(BindingBuilder.bind(fila).to(eventos).with(routingKey));
        }
        return new Declarables(declaraveis);
    }

    /** Ack manual: o consumidor confirma só depois de a transação do processamento ser gravada. */
    @Bean(FABRICA_LISTENER)
    SimpleRabbitListenerContainerFactory fabricaListenerTelemetria(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Value("${telemetry.mensageria.prefetch}") int prefetch) {
        SimpleRabbitListenerContainerFactory fabrica = new SimpleRabbitListenerContainerFactory();
        configurer.configure(fabrica, connectionFactory);
        fabrica.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        fabrica.setPrefetchCount(prefetch);
        fabrica.setDefaultRequeueRejected(false);
        return fabrica;
    }
}
