package com.seniorinsight.telemetry.adapters.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.AcessoNegadoException;
import com.seniorinsight.telemetry.application.porta.entrada.ConsultarStatusAparelho;
import com.seniorinsight.telemetry.application.porta.entrada.StatusAparelho;
import com.seniorinsight.telemetry.domain.consentimento.TipoDado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StatusAparelhoController.class,
        properties = "KEYCLOAK_ISSUER_URI=http://keycloak.invalid/realms/seniorinsight")
@Import({SegurancaConfig.class, TratamentoErros.class})
class StatusAparelhoControllerTest {

    private static final UUID IDOSO = UUID.fromString("56e67608-4907-4b24-b13d-15a52a58ae46");
    // O gateway entrega /api/v1/telemetry/idosos/{id}/status como /v1/idosos/{id}/status.
    private static final String ROTA = "/v1/idosos/" + IDOSO + "/status";

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean ConsultarStatusAparelho consulta;

    @Test
    void respondeOStatusAparelhoDoContratoEmCamelCase() throws Exception {
        when(consulta.consultar(IDOSO, "sub")).thenReturn(new StatusAparelho(IDOSO,
                Instant.parse("2026-11-10T14:30:00Z"), 42, "1.0.4", "Samsung Galaxy A15", "Android 14",
                List.of(TipoDado.BATIMENTOS, TipoDado.PASSOS)));

        mvc.perform(get(ROTA).with(jwt().jwt(t -> t.subject("sub"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idosoId").value(IDOSO.toString()))
                .andExpect(jsonPath("$.ultimaSincronizacao").value("2026-11-10T14:30:00Z"))
                .andExpect(jsonPath("$.nivelBateria").value(42))
                .andExpect(jsonPath("$.versaoApp").value("1.0.4"))
                .andExpect(jsonPath("$.modeloCelular").value("Samsung Galaxy A15"))
                .andExpect(jsonPath("$.sistemaOperacional").value("Android 14"))
                .andExpect(jsonPath("$.tiposAtivos[0]").value("BATIMENTOS"))
                .andExpect(jsonPath("$.tiposAtivos[1]").value("PASSOS"));
    }

    @Test
    void foraDaRedeDoIdosoDa403() throws Exception {
        when(consulta.consultar(IDOSO, "estranho")).thenThrow(new AcessoNegadoException(IDOSO));

        mvc.perform(get(ROTA).with(jwt().jwt(t -> t.subject("estranho")))).andExpect(status().isForbidden());
    }

    @Test
    void semTokenDa401() throws Exception {
        mvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
    }

    @Test
    void rotaComPrefixoDoGatewayNaoExiste() throws Exception {
        mvc.perform(get("/api/v1/telemetry/idosos/" + IDOSO + "/status").with(jwt()))
                .andExpect(status().isNotFound());
    }
}
