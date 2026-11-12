package com.seniorinsight.telemetry.adapters.out.openwearables;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.OpenWearablesIndisponivelException;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * Adaptador da API REST do Open Wearables ({@code /api/v1}). Cada requisição passa pelo circuit breaker e, por fora
 * dele, pelo retry: uma requisição que esgota as tentativas conta várias falhas no circuito.
 */
class ClienteOpenWearables implements OpenWearables {

    static final String ORIGEM_PADRAO = "open_wearables";
    static final int TAMANHO_PAGINA = 100;

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    ClienteOpenWearables(RestClient restClient, CircuitBreaker circuitBreaker, Retry retry) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    @Override
    public UUID provisionarUsuario() {
        // Sem dados pessoais no Open Wearables: o vínculo idoso ↔ usuário fica só no telemetry-service.
        UsuarioCriado usuario = chamar("provisionar usuário", () -> restClient.post()
                .uri("/api/v1/users")
                .body(Map.of())
                .retrieve()
                .body(UsuarioCriado.class));
        return usuario.id();
    }

    @Override
    public List<Leitura> buscarLeituras(UUID usuarioOpenWearables, Instant inicio, Instant fim) {
        List<Leitura> leituras = new ArrayList<>();
        String cursor = null;
        do {
            String cursorAtual = cursor;
            PaginaAmostras pagina = chamar("buscar leituras", () -> restClient.get()
                    .uri(uri -> urlSerieTemporal(uri, usuarioOpenWearables, inicio, fim, cursorAtual))
                    .retrieve()
                    .body(PaginaAmostras.class));
            if (pagina == null || pagina.data() == null) {
                break;
            }
            pagina.data().stream().map(ClienteOpenWearables::paraLeitura).forEach(leituras::add);
            cursor = pagina.pagination() != null && pagina.pagination().hasMore()
                    ? pagina.pagination().nextCursor()
                    : null;
        } while (cursor != null);
        return leituras;
    }

    private static URI urlSerieTemporal(UriBuilder uri, UUID usuario, Instant inicio, Instant fim,
                                    String cursor) {
        uri.path("/api/v1/users/{usuario}/timeseries")
                .queryParam("start_time", inicio)
                .queryParam("end_time", fim)
                .queryParam("limit", TAMANHO_PAGINA);
        if (cursor != null) {
            uri.queryParam("cursor", cursor);
        }
        return uri.build(usuario);
    }

    private <T> T chamar(String operacao, Supplier<T> requisicao) {
        Supplier<T> protegida = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, requisicao));
        try {
            return protegida.get();
        } catch (CallNotPermittedException e) {
            throw new OpenWearablesIndisponivelException("Open Wearables com circuito aberto ao " + operacao, e);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new OpenWearablesIndisponivelException("Open Wearables indisponível ao " + operacao, e);
        }
    }

    private static Leitura paraLeitura(Amostra amostra) {
        String origem = amostra.source() != null && amostra.source().provider() != null
                ? amostra.source().provider()
                : ORIGEM_PADRAO;
        return new Leitura(amostra.type(), amostra.timestamp(), amostra.value(), amostra.unit(), origem);
    }

    record UsuarioCriado(UUID id) {
    }

    record PaginaAmostras(List<Amostra> data, Paginacao pagination) {
    }

    record Paginacao(@JsonProperty("next_cursor") String nextCursor,
                     @JsonProperty("has_more") boolean hasMore) {
    }

    record Amostra(Instant timestamp, String type, double value, String unit, Fonte source) {
    }

    record Fonte(String provider) {
    }
}
