package com.seniorinsight.telemetry.adapters.out.openwearables;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
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
    public Optional<UUID> buscarUsuarioPorReferencia(String referencia) {
        PaginaUsuarios pagina = chamar("localizar usuário", () -> restClient.get()
                .uri(uri -> uri.path("/api/v1/users").queryParam("external_user_id", referencia).build())
                .retrieve().body(PaginaUsuarios.class));
        if (pagina == null || pagina.items() == null || pagina.items().size() > 1
                || (pagina.total() != null && pagina.total() > 1)) {
            throw falhaVinculo();
        }
        return pagina.items().stream().map(usuario -> validarReferencia(usuario, referencia)).findFirst();
    }

    @Override
    public UUID provisionarUsuario(String referencia) {
        try {
            UsuarioCriado usuario = chamar("provisionar usuário", () -> restClient.post()
                    .uri("/api/v1/users")
                    .body(Map.of("external_user_id", referencia))
                    .retrieve().body(UsuarioCriado.class));
            return validarReferencia(usuario, referencia);
        } catch (HttpClientErrorException.Conflict conflito) {
            return buscarUsuarioPorReferencia(referencia).orElseThrow(ClienteOpenWearables::falhaVinculo);
        }
    }

    private static UUID validarReferencia(UsuarioCriado usuario, String referencia) {
        if (usuario == null || usuario.id() == null || !referencia.equals(usuario.referencia())) {
            throw falhaVinculo();
        }
        return usuario.id();
    }

    private static OpenWearablesIndisponivelException falhaVinculo() {
        return new OpenWearablesIndisponivelException("Vínculo com usuário do Open Wearables não confirmado", null);
    }

    @Override
    public void excluirUsuario(UUID usuarioOpenWearables) {
        try {
            var resposta = chamar("excluir usuário", () -> restClient.delete()
                    .uri("/api/v1/users/{usuario}", usuarioOpenWearables)
                    .retrieve()
                    .toEntity(UsuarioCriado.class));
            if (resposta.getStatusCode().value() != 204
                    && (resposta.getStatusCode().value() != 200 || resposta.getBody() == null
                        || !usuarioOpenWearables.equals(resposta.getBody().id()))) {
                throw falhaExclusao();
            }
        } catch (HttpClientErrorException.NotFound e) {
            if (!usuarioJaAusente(e, usuarioOpenWearables)) {
                throw falhaExclusao();
            }
        } catch (RestClientException | OpenWearablesIndisponivelException e) {
            throw falhaExclusao();
        }
    }

    private static boolean usuarioJaAusente(HttpClientErrorException.NotFound erro, UUID usuario) {
        try {
            var resposta = erro.getResponseBodyAs(ErroExclusao.class);
            return resposta != null && ("User with ID: " + usuario + " not found.").equals(resposta.detail());
        } catch (RestClientException | IllegalStateException e) {
            return false;
        }
    }

    private static OpenWearablesIndisponivelException falhaExclusao() {
        return new OpenWearablesIndisponivelException("Exclusão não confirmada pelo Open Wearables", null);
    }

    record ErroExclusao(String detail) {
    }

    @Override
    public List<Leitura> buscarLeituras(UUID usuarioOpenWearables, Instant inicio, Instant fim, Set<String> tipos) {
        List<Leitura> leituras = new ArrayList<>();
        var series = tipos.stream().filter(tipo -> !"sleep".equals(tipo)).sorted().toList();
        if (!series.isEmpty()) {
            buscarSeries(usuarioOpenWearables, inicio, fim, Set.copyOf(series), String.join(",", series), leituras);
        }
        if (tipos.contains("sleep")) {
            buscarSono(usuarioOpenWearables, inicio, fim, leituras);
        }
        return leituras;
    }

    private void buscarSeries(UUID usuario, Instant inicio, Instant fim, Set<String> tipos, String filtro,
                              List<Leitura> leituras) {
        String cursor = null;
        do {
            String cursorAtual = cursor;
            PaginaAmostras pagina = chamar("buscar leituras", () -> restClient.get()
                    .uri(uri -> urlSerieTemporal(uri, usuario, inicio, fim, filtro, cursorAtual))
                    .retrieve()
                    .body(PaginaAmostras.class));
            if (pagina == null || pagina.data() == null) {
                throw new OpenWearablesIndisponivelException("Resposta inválida para séries do Open Wearables", null);
            }
            pagina.data().stream().filter(amostra -> tipos.contains(amostra.type()))
                    .map(ClienteOpenWearables::paraLeitura).forEach(leituras::add);
            cursor = proximaPagina(pagina.pagination(), cursorAtual);
        } while (cursor != null);
    }

    private void buscarSono(UUID usuario, Instant inicio, Instant fim, List<Leitura> leituras) {
        String cursor = null;
        do {
            String cursorAtual = cursor;
            PaginaSono pagina = chamar("buscar sessões de sono", () -> restClient.get().uri(uri -> {
                uri.path("/api/v1/users/{usuario}/events/sleep")
                        .queryParam("start_date", inicio).queryParam("end_date", fim)
                        .queryParam("source_selection", "per_session").queryParam("limit", TAMANHO_PAGINA);
                if (cursorAtual != null) {
                    uri.queryParam("cursor", cursorAtual);
                }
                return uri.build(usuario);
            }).retrieve().body(PaginaSono.class));
            if (pagina == null || pagina.data() == null) {
                throw new OpenWearablesIndisponivelException("Resposta inválida para sono do Open Wearables", null);
            }
            pagina.data().stream().filter(sessao -> sessao.finalizada() && sessao.inicio() != null
                            && !sessao.inicio().isBefore(inicio) && sessao.fim() != null && sessao.segundos() != null)
                    .map(sessao -> new Leitura("sleep", sessao.fim(), sessao.segundos(), "s", origem(sessao.source())))
                    .forEach(leituras::add);
            cursor = proximaPagina(pagina.pagination(), cursorAtual);
        } while (cursor != null);
    }

    private static String proximaPagina(Paginacao paginacao, String anterior) {
        if (paginacao == null || !paginacao.hasMore()) {
            return null;
        }
        String cursor = paginacao.nextCursor();
        if (cursor == null || cursor.isBlank() || cursor.equals(anterior)) {
            throw new OpenWearablesIndisponivelException("Paginação inválida no Open Wearables", null);
        }
        return cursor;
    }

    private static URI urlSerieTemporal(UriBuilder uri, UUID usuario, Instant inicio, Instant fim,
                                    String tipos, String cursor) {
        uri.path("/api/v1/users/{usuario}/timeseries")
                .queryParam("start_time", inicio)
                .queryParam("end_time", fim)
                .queryParam("types", tipos)
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
        return new Leitura(amostra.type(), amostra.timestamp(), amostra.value(), amostra.unit(), origem(amostra.source()));
    }

    private static String origem(Fonte fonte) {
        return fonte != null && fonte.provider() != null ? fonte.provider() : ORIGEM_PADRAO;
    }

    record UsuarioCriado(UUID id, @JsonProperty("external_user_id") String referencia) {
    }

    record PaginaUsuarios(List<UsuarioCriado> items, Integer total) {
    }

    record PaginaAmostras(List<Amostra> data, Paginacao pagination) {
    }

    record PaginaSono(List<SessaoSono> data, Paginacao pagination) {
    }

    record SessaoSono(@JsonProperty("start_time") Instant inicio, @JsonProperty("end_time") Instant fim,
                     @JsonProperty("sleep_duration_seconds") Integer segundos,
                     @JsonProperty("is_final") boolean finalizada, Fonte source) {
    }

    record Paginacao(@JsonProperty("next_cursor") String nextCursor,
                     @JsonProperty("has_more") boolean hasMore) {
    }

    record Amostra(Instant timestamp, String type, double value, String unit, Fonte source) {
    }

    record Fonte(String provider) {
    }
}
