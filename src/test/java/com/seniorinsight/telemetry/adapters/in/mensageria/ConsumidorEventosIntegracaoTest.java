package com.seniorinsight.telemetry.adapters.in.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.TratadoresTeste;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Fila {@code telemetry.q} com RabbitMQ real: idempotência pelo id do evento, ack manual e dead-letter. */
@TesteIntegracao
class ConsumidorEventosIntegracaoTest {

    private static final Duration PRAZO = Duration.ofSeconds(15);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TratadoresTeste.Contador contador;

    @Test
    void eventoEntregueDuasVezesEhTratadoUmaVez() {
        UUID eventoId = UUID.randomUUID();

        enviarParaAFila(mensagem(eventoId, TratadoresTeste.TIPO_CONTADO));
        enviarParaAFila(mensagem(eventoId, TratadoresTeste.TIPO_CONTADO));

        await().atMost(PRAZO).untilAsserted(() -> {
            assertThat(contador.vezes(eventoId)).isEqualTo(1);
            assertThat(registrado(eventoId)).isTrue();
        });
        // Dá tempo para a segunda entrega ser consumida: ela é confirmada sem tratar de novo.
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(contador.vezes(eventoId)).isEqualTo(1));
    }

    @Test
    void eventoPublicadoNaSiEventsComRoutingKeyAssinadaChegaNaFila() {
        UUID eventoId = UUID.randomUUID();
        Message mensagem = MessageBuilder.withBody("{}".getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(eventoId.toString())
                .build();

        rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "consent.changed", mensagem);

        await().atMost(PRAZO).untilAsserted(() -> assertThat(jdbc.queryForList(
                "SELECT tipo_evento FROM evento_recebido WHERE id = ?", String.class, eventoId))
                .containsExactly("consent.changed"));
    }

    @Test
    void falhaNoTratadorMandaParaADeadLetterSemRegistrarOEvento() {
        UUID eventoId = UUID.randomUUID();

        enviarParaAFila(mensagem(eventoId, TratadoresTeste.TIPO_FALHA));

        Message morta = aguardarNaDeadLetter(m -> eventoId.toString().equals(m.getMessageProperties().getMessageId()));
        assertThat(morta.getMessageProperties().getType()).isEqualTo(TratadoresTeste.TIPO_FALHA);
        assertThat(registrado(eventoId)).isFalse();
    }

    @Test
    void mensagemSemMessageIdVaiParaADeadLetter() {
        String marcador = UUID.randomUUID().toString();
        Message mensagem = MessageBuilder.withBody("{}".getBytes(StandardCharsets.UTF_8))
                .setType(TratadoresTeste.TIPO_CONTADO)
                .setHeader("marcador", marcador)
                .build();

        enviarParaAFila(mensagem);

        aguardarNaDeadLetter(m -> marcador.equals(m.getMessageProperties().getHeader("marcador")));
    }

    private void enviarParaAFila(Message mensagem) {
        rabbitTemplate.send("", MensageriaConfig.FILA, mensagem);
    }

    private boolean registrado(UUID eventoId) {
        return jdbc.queryForObject("SELECT count(*) FROM evento_recebido WHERE id = ?", Integer.class, eventoId) == 1;
    }

    /** Lê a dead-letter até achar a mensagem esperada; as de outros testes são descartadas. */
    private Message aguardarNaDeadLetter(Predicate<Message> esperada) {
        long prazo = System.nanoTime() + PRAZO.toNanos();
        while (System.nanoTime() < prazo) {
            Message mensagem = rabbitTemplate.receive(MensageriaConfig.FILA_DEAD_LETTER, 1_000);
            if (mensagem != null && esperada.test(mensagem)) {
                return mensagem;
            }
        }
        throw new AssertionError("mensagem esperada não chegou em " + MensageriaConfig.FILA_DEAD_LETTER);
    }

    private static Message mensagem(UUID eventoId, String tipo) {
        return MessageBuilder.withBody("{}".getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(eventoId.toString())
                .setType(tipo)
                .build();
    }
}
