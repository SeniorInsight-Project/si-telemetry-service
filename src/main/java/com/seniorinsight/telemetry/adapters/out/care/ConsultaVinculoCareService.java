package com.seniorinsight.telemetry.adapters.out.care;

import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.ConsultaVinculo;
import com.seniorinsight.telemetry.application.porta.saida.VinculoIndisponivelException;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Consulta o endpoint interno do care-service {@code GET /internal/idosos/{idosoId}/vinculo/{sub}}
 * (si-care-service#74): 200 com {@code {"papel": "FAMILIAR_ADMINISTRADOR"}} quando há vínculo, 404 quando não há.
 *
 * <p>Repassa o token do usuário da requisição atual, se houver, para o care-service autenticar a chamada.
 */
class ConsultaVinculoCareService implements ConsultaVinculo {

    private final RestClient restClient;

    ConsultaVinculoCareService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<Papel> papelDoUsuario(UUID idosoId, String sub) {
        try {
            Vinculo vinculo = restClient.get()
                    .uri("/internal/idosos/{idosoId}/vinculo/{sub}", idosoId, sub)
                    .headers(ConsultaVinculoCareService::repassarToken)
                    .retrieve()
                    .body(Vinculo.class);
            return Optional.ofNullable(vinculo).map(Vinculo::papel);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new VinculoIndisponivelException(
                    "Falha ao consultar o vínculo do usuário com o idoso " + idosoId + " no care-service", e);
        }
    }

    private static void repassarToken(HttpHeaders headers) {
        if (SecurityContextHolder.getContext().getAuthentication()
                instanceof AbstractOAuth2TokenAuthenticationToken<?> autenticacao) {
            headers.setBearerAuth(autenticacao.getToken().getTokenValue());
        }
    }

    record Vinculo(Papel papel) {
    }
}
