package com.seniorinsight.telemetry.adapters.out.openwearables;

import java.net.http.HttpClient;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP do Open Wearables com timeout de conexão e de leitura. Retry e circuit breaker são a instância
 * {@code openWearables} do Resilience4j ({@code resilience4j.*} no application.yml).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropriedadesOpenWearables.class)
class OpenWearablesConfig {

    static final String INSTANCIA_RESILIENCIA = "openWearables";
    static final String HEADER_API_KEY = "X-Open-Wearables-API-Key";

    @Bean
    ClienteOpenWearables clienteOpenWearables(PropriedadesOpenWearables propriedades,
                                              RestClient.Builder builder,
                                              CircuitBreakerRegistry circuitBreakers,
                                              RetryRegistry retries) {
        return new ClienteOpenWearables(
                restClient(builder, propriedades),
                circuitBreakers.circuitBreaker(INSTANCIA_RESILIENCIA),
                retries.retry(INSTANCIA_RESILIENCIA));
    }

    static RestClient restClient(RestClient.Builder builder, PropriedadesOpenWearables propriedades) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(propriedades.timeoutConexao())
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(propriedades.timeoutLeitura());

        builder.baseUrl(propriedades.baseUrl()).requestFactory(fabrica);
        if (propriedades.apiKey() != null && !propriedades.apiKey().isBlank()) {
            builder.defaultHeader(HEADER_API_KEY, propriedades.apiKey());
        }
        return builder.build();
    }
}
