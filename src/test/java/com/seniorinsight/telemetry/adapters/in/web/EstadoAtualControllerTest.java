package com.seniorinsight.telemetry.adapters.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.AcessoNegadoException;
import com.seniorinsight.telemetry.application.porta.entrada.ConsultarEstadoAtual;
import com.seniorinsight.telemetry.application.porta.entrada.EstadoAtual;
import com.seniorinsight.telemetry.application.porta.saida.VinculoIndisponivelException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EstadoAtualController.class,
        properties = "KEYCLOAK_ISSUER_URI=http://keycloak.invalid/realms/seniorinsight")
@Import({SegurancaConfig.class, TratamentoErros.class})
class EstadoAtualControllerTest {

    private static final UUID IDOSO = UUID.fromString("56e67608-4907-4b24-b13d-15a52a58ae46");
    // O gateway entrega /api/v1/telemetry/idosos/{id}/hoje como /v1/idosos/{id}/hoje.
    private static final String ROTA = "/v1/idosos/" + IDOSO + "/hoje";

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean ConsultarEstadoAtual consulta;

    @Test
    void respondeOEstadoAtualDoContratoEmCamelCase() throws Exception {
        when(consulta.consultar(IDOSO, "sub")).thenReturn(new EstadoAtual(IDOSO, 3200L, 4000, 78, 64,
                Instant.parse("2026-11-10T14:30:00Z")));

        mvc.perform(get(ROTA).with(jwt().jwt(t -> t.subject("sub"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idosoId").value(IDOSO.toString()))
                .andExpect(jsonPath("$.passosHoje").value(3200))
                .andExpect(jsonPath("$.metaPassos").value(4000))
                .andExpect(jsonPath("$.frequenciaCardiaca").value(78))
                .andExpect(jsonPath("$.frequenciaCardiacaRepouso").value(64))
                .andExpect(jsonPath("$.coletadoEm").value("2026-11-10T14:30:00Z"));
    }

    @Test
    void tipoRevogadoVaiComoNullENaoSomeDoJson() throws Exception {
        when(consulta.consultar(IDOSO, "sub")).thenReturn(new EstadoAtual(IDOSO, null, 4000, null, null, null));

        mvc.perform(get(ROTA).with(jwt().jwt(t -> t.subject("sub"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passosHoje").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.frequenciaCardiaca").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.coletadoEm").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasKey("passosHoje")))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasKey("frequenciaCardiaca")))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasKey("coletadoEm")));
    }

    @Test
    void foraDaRedeDoIdosoDa403() throws Exception {
        when(consulta.consultar(IDOSO, "estranho")).thenThrow(new AcessoNegadoException(IDOSO));

        mvc.perform(get(ROTA).with(jwt().jwt(t -> t.subject("estranho"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void careForaDoArDa503() throws Exception {
        when(consulta.consultar(IDOSO, "sub")).thenThrow(new VinculoIndisponivelException("fora", null));

        mvc.perform(get(ROTA).with(jwt().jwt(t -> t.subject("sub"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void semTokenDa401() throws Exception {
        mvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
    }

    @Test
    void idosoIdQueNaoEUuidDa400() throws Exception {
        mvc.perform(get("/v1/idosos/nao-e-uuid/hoje").with(jwt())).andExpect(status().isBadRequest());
    }

    @Test
    void rotaComPrefixoDoGatewayNaoExiste() throws Exception {
        mvc.perform(get("/api/v1/telemetry/idosos/" + IDOSO + "/hoje").with(jwt()))
                .andExpect(status().isNotFound());
    }
}
