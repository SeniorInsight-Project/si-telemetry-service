package com.seniorinsight.telemetry.adapters.out.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.domain.evento.EventoDominio;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/** Caminho completo: caso de uso grava na outbox, o job agendado publica no RabbitMQ e marca a linha. */
@TesteIntegracao
class RelayOutboxIntegracaoTest {

    @Autowired
    PublicadorEventos publicador;

    @Autowired
    OutboxEventoRepository repositorio;

    @Autowired
    TransactionTemplate transacao;

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void publicaNaExchangeSiEventsComRoutingKeyIgualAoTipoDoEventoEMarcaPublicado() throws Exception {
        Queue fila = amqpAdmin.declareQueue();
        amqpAdmin.declareBinding(BindingBuilder.bind(fila)
                .to(new TopicExchange(OutboxConfig.EXCHANGE_EVENTOS))
                .with("telemetry.ingested"));
        UUID idosoId = UUID.randomUUID();

        transacao.executeWithoutResult(status -> publicador.publicar(new TelemetriaIngerida(idosoId, "2026-10-31")));

        Message mensagem = rabbitTemplate.receive(fila.getName(), Duration.ofSeconds(15).toMillis());
        assertThat(mensagem).as("mensagem publicada pelo relay").isNotNull();
        MessageProperties propriedades = mensagem.getMessageProperties();
        assertThat(propriedades.getReceivedExchange()).isEqualTo("si.events");
        assertThat(propriedades.getReceivedRoutingKey()).isEqualTo("telemetry.ingested");
        assertThat(propriedades.getType()).isEqualTo("telemetry.ingested");
        assertThat(propriedades.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(propriedades.getAppId()).isEqualTo("telemetry-service");
        assertThat(propriedades.<String>getHeader("tipo_agregado")).isEqualTo("idoso");
        assertThat(propriedades.<String>getHeader("agregado_id")).isEqualTo(idosoId.toString());
        assertThat(objectMapper.readTree(mensagem.getBody()))
                .isEqualTo(objectMapper.createObjectNode().put("idosoId", idosoId.toString()).put("dia", "2026-10-31"));

        UUID eventoId = UUID.fromString(propriedades.getMessageId());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            OutboxEvento evento = repositorio.findById(eventoId).orElseThrow();
            assertThat(evento.getAgregadoId()).isEqualTo(idosoId);
            assertThat(evento.getPublicadoEm()).isNotNull();
            assertThat(evento.getTentativas()).isZero();
        });
    }

    @Test
    void eventoNaoEhGravadoQuandoATransacaoDoCasoDeUsoEhDesfeita() {
        UUID idosoId = UUID.randomUUID();

        transacao.executeWithoutResult(status -> {
            publicador.publicar(new TelemetriaIngerida(idosoId, "2026-10-30"));
            status.setRollbackOnly();
        });

        assertThat(repositorio.findAll()).noneMatch(evento -> evento.getAgregadoId().equals(idosoId));
    }

    @Test
    void publicarExigeTransacaoAtiva() {
        assertThatThrownBy(() -> publicador.publicar(new TelemetriaIngerida(UUID.randomUUID(), "2026-10-29")))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    record TelemetriaIngerida(UUID idosoId, String dia) implements EventoDominio {

        @Override
        public String tipoAgregado() {
            return "idoso";
        }

        @Override
        public UUID agregadoId() {
            return idosoId;
        }

        @Override
        public String tipoEvento() {
            return "telemetry.ingested";
        }
    }
}
