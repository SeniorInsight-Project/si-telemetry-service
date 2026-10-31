package com.seniorinsight.telemetry.adapters.out.care;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropriedadesCareService.class)
class CareServiceConfig {

    @Bean
    ConsultaVinculoCareService consultaVinculoCareService(PropriedadesCareService propriedades,
                                                          RestClient.Builder builder) {
        return new ConsultaVinculoCareService(restClient(builder, propriedades));
    }

    static RestClient restClient(RestClient.Builder builder, PropriedadesCareService propriedades) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(propriedades.timeoutConexao())
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(propriedades.timeoutLeitura());
        return builder.baseUrl(propriedades.baseUrl()).requestFactory(fabrica).build();
    }
}
