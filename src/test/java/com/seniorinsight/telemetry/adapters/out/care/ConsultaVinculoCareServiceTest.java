package com.seniorinsight.telemetry.adapters.out.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;

import com.seniorinsight.telemetry.ServidorHttpFalso;
import com.seniorinsight.telemetry.application.porta.saida.VinculoIndisponivelException;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

class ConsultaVinculoCareServiceTest {

    private static final UUID IDOSO = UUID.fromString("4f1c2a9e-0000-4000-8000-000000000001");
    private static final String SUB = "sub-123";

    private final ServidorHttpFalso servidor = ServidorHttpFalso.iniciar();
    private final ConsultaVinculoCareService consulta = new ConsultaVinculoCareService(CareServiceConfig.restClient(
            RestClient.builder(),
            new PropriedadesCareService(servidor.url(), Duration.ofSeconds(1), Duration.ofMillis(300))));

    @AfterEach
    void limpar() {
        servidor.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    void devolveOPapelQuandoHaVinculo() {
        servidor.responder(200, "{\"papel\": \"FAMILIAR_APOIO\"}");

        assertThat(consulta.papelDoUsuario(IDOSO, SUB)).contains(Papel.FAMILIAR_APOIO);
        assertThat(servidor.requisicoes().getFirst().caminho())
                .isEqualTo("/internal/idosos/" + IDOSO + "/vinculo/" + SUB);
    }

    @Test
    void semVinculoDevolveVazio() {
        servidor.responder(404, "{}");

        assertThat(consulta.papelDoUsuario(IDOSO, SUB)).isEmpty();
    }

    @Test
    void erroDoCareServiceViraIndisponivel() {
        servidor.responder(500, "{}");

        assertThatThrownBy(() -> consulta.papelDoUsuario(IDOSO, SUB))
                .isInstanceOf(VinculoIndisponivelException.class);
    }

    @Test
    void timeoutViraIndisponivel() {
        servidor.responderComAtraso(Duration.ofSeconds(2), 200, "{\"papel\": \"MEDICO\"}");

        assertThatThrownBy(() -> consulta.papelDoUsuario(IDOSO, SUB))
                .isInstanceOf(VinculoIndisponivelException.class);
    }

    @Test
    void repassaOTokenDoUsuarioDaRequisicao() {
        Jwt jwt = Jwt.withTokenValue("token-abc").header("alg", "RS256").subject(SUB).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        servidor.responder(200, "{\"papel\": \"FAMILIAR_ADMINISTRADOR\"}");

        consulta.papelDoUsuario(IDOSO, SUB);

        assertThat(servidor.requisicoes().getFirst().header("Authorization")).isEqualTo("Bearer token-abc");
    }
}
