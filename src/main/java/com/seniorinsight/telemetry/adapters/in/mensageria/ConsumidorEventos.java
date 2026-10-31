package com.seniorinsight.telemetry.adapters.in.mensageria;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.rabbitmq.client.Channel;
import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import com.seniorinsight.telemetry.application.porta.entrada.ReceberEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome a fila {@code telemetry.q} com ack manual.
 *
 * <p>O id do evento é o {@code message_id} (id da outbox de quem publicou) e o tipo é a propriedade {@code type}, ou a
 * routing key quando ela não vier. Processou (ou já tinha processado): ack. Mensagem sem id válido ou falha no
 * processamento: reject sem requeue, e o broker manda para {@code telemetry.q.dlq}.
 */
@Component
class ConsumidorEventos {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final ReceberEvento receberEvento;

    ConsumidorEventos(ReceberEvento receberEvento) {
        this.receberEvento = receberEvento;
    }

    @RabbitListener(queues = MensageriaConfig.FILA, containerFactory = MensageriaConfig.FABRICA_LISTENER)
    void receber(Message mensagem, Channel canal) throws IOException {
        long tag = mensagem.getMessageProperties().getDeliveryTag();

        EventoRecebido evento;
        try {
            evento = paraEvento(mensagem);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Mensagem inválida em {}, enviada para a dead-letter: {}", MensageriaConfig.FILA, e.getMessage());
            canal.basicReject(tag, false);
            return;
        }

        try {
            receberEvento.receber(evento);
        } catch (RuntimeException e) {
            log.error("Falha ao processar o evento {} ({}), enviado para a dead-letter", evento.id(), evento.tipo(), e);
            canal.basicReject(tag, false);
            return;
        }
        canal.basicAck(tag, false);
    }

    static EventoRecebido paraEvento(Message mensagem) {
        MessageProperties propriedades = mensagem.getMessageProperties();
        if (propriedades.getMessageId() == null) {
            throw new IllegalArgumentException("mensagem sem message_id");
        }
        String tipo = propriedades.getType() != null ? propriedades.getType() : propriedades.getReceivedRoutingKey();
        return new EventoRecebido(
                UUID.fromString(propriedades.getMessageId()),
                tipo,
                new String(mensagem.getBody(), StandardCharsets.UTF_8));
    }
}
