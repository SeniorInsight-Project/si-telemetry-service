package com.seniorinsight.telemetry.adapters.out.openwearables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.telemetry.ServidorHttpFalso;
import com.seniorinsight.telemetry.application.porta.saida.OpenWearablesIndisponivelException;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Contra um servidor HTTP local, com retry e circuit breaker configurados como no application.yml, só que menores. */
class ClienteOpenWearablesTest {

    private static final UUID USUARIO = UUID.fromString("0b8f3c52-7d1e-4a3b-9c1d-2e5f6a7b8c9d");
    private static final Instant INICIO = Instant.parse("2026-10-31T00:00:00Z");
    private static final Instant FIM = Instant.parse("2026-10-31T00:15:00Z");

    private final ServidorHttpFalso servidor = ServidorHttpFalso.iniciar();

    private final CircuitBreaker circuitBreaker = CircuitBreaker.of("openWearables", CircuitBreakerConfig.custom()
            .slidingWindowSize(4)
            .minimumNumberOfCalls(4)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .recordExceptions(ResourceAccessException.class, HttpServerErrorException.class)
            .build());

    private final Retry retry = Retry.of("openWearables", RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(10))
            .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
            .build());

    private final ClienteOpenWearables cliente = new ClienteOpenWearables(
            OpenWearablesConfig.restClient(RestClient.builder(), new PropriedadesOpenWearables(
                    true, servidor.url(), "chave-teste", Duration.ofSeconds(1), Duration.ofMillis(300))),
            circuitBreaker, retry);

    @AfterEach
    void pararServidor() {
        servidor.close();
    }

    @Test
    void provisionaUsuarioEnviandoAChaveNoHeader() {
        servidor.responder(201, """
                {"id": "%s", "created_at": "2026-10-31T10:00:00Z", "has_active_connection": false}
                """.formatted(USUARIO));

        assertThat(cliente.provisionarUsuario()).isEqualTo(USUARIO);

        ServidorHttpFalso.Requisicao requisicao = servidor.requisicoes().getFirst();
        assertThat(requisicao.metodo()).isEqualTo("POST");
        assertThat(requisicao.caminho()).isEqualTo("/api/v1/users");
        assertThat(requisicao.header("X-Open-Wearables-API-Key")).isEqualTo("chave-teste");
    }

    @Test
    void buscaLeiturasDeTodasAsPaginas() {
        servidor.responder(200, """
                {"data": [{"timestamp": "2026-10-31T00:05:00Z", "type": "heart_rate", "value": 72, "unit": "bpm",
                           "source": {"provider": "apple"}}],
                 "pagination": {"next_cursor": "c2", "has_more": true}}
                """);
        servidor.responder(200, """
                {"data": [{"timestamp": "2026-10-31T00:10:00Z", "type": "steps", "value": 120, "unit": "count"}],
                 "pagination": {"next_cursor": null, "has_more": false}}
                """);

        assertThat(cliente.buscarLeituras(USUARIO, INICIO, FIM)).containsExactly(
                new Leitura("heart_rate", Instant.parse("2026-10-31T00:05:00Z"), 72, "bpm", "apple"),
                new Leitura("steps", Instant.parse("2026-10-31T00:10:00Z"), 120, "count", "open_wearables"));

        assertThat(servidor.requisicoes()).hasSize(2);
        ServidorHttpFalso.Requisicao primeira = servidor.requisicoes().get(0);
        assertThat(primeira.caminho()).isEqualTo("/api/v1/users/" + USUARIO + "/timeseries");
        assertThat(primeira.query()).contains("start_time=2026-10-31T00:00:00Z", "end_time=2026-10-31T00:15:00Z")
                .doesNotContain("cursor");
        assertThat(servidor.requisicoes().get(1).query()).contains("cursor=c2");
    }

    @Test
    void repeteQuandoOServidorFalha() {
        servidor.responder(503, "{}").responder(502, "{}").responder(201, "{\"id\": \"%s\"}".formatted(USUARIO));

        assertThat(cliente.provisionarUsuario()).isEqualTo(USUARIO);
        assertThat(servidor.requisicoes()).hasSize(3);
    }

    @Test
    void naoRepeteErroDoCliente() {
        servidor.responder(404, "{\"detail\": \"User not found\"}");

        assertThatThrownBy(() -> cliente.buscarLeituras(USUARIO, INICIO, FIM))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThat(servidor.requisicoes()).hasSize(1);
    }

    @Test
    void timeoutDeLeituraViraIndisponivelDepoisDasTentativas() {
        for (int i = 0; i < 3; i++) {
            servidor.responderComAtraso(Duration.ofSeconds(2), 201, "{\"id\": \"%s\"}".formatted(USUARIO));
        }

        assertThatThrownBy(cliente::provisionarUsuario)
                .isInstanceOf(OpenWearablesIndisponivelException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
        assertThat(servidor.requisicoes()).hasSize(3);
    }

    @Test
    void circuitoAbreDepoisDeFalhasSeguidasEParaDeChamarOServidor() {
        assertThatThrownBy(cliente::provisionarUsuario).isInstanceOf(OpenWearablesIndisponivelException.class);
        assertThatThrownBy(cliente::provisionarUsuario)
                .isInstanceOf(OpenWearablesIndisponivelException.class)
                .hasCauseInstanceOf(CallNotPermittedException.class);
        assertThatThrownBy(cliente::provisionarUsuario)
                .isInstanceOf(OpenWearablesIndisponivelException.class)
                .hasCauseInstanceOf(CallNotPermittedException.class);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        // 3 tentativas na primeira chamada + 1 na segunda, que abre o circuito; a terceira nem sai.
        assertThat(servidor.requisicoes()).hasSize(4);
    }
}
