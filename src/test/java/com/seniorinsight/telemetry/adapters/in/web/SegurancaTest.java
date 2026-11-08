package com.seniorinsight.telemetry.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seniorinsight.telemetry.TesteIntegracao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Ainda não há rotas de negócio: uma rota inexistente mostra se a requisição passou pela autenticação. */
@TesteIntegracao
class SegurancaTest {

    private static final String ROTA_PROTEGIDA = "/api/v1/telemetry/idosos/4f1c2a9e-0000-4000-8000-000000000001/status";

    @Autowired
    MockMvc mvc;

    @Test
    void rotaSemTokenRetorna401() throws Exception {
        mvc.perform(get(ROTA_PROTEGIDA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void rotaComTokenPassaPelaAutenticacaoSemCriarSessao() throws Exception {
        mvc.perform(get(ROTA_PROTEGIDA).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(resultado -> assertThat(resultado.getRequest().getSession(false)).isNull());
    }

    @Test
    void healthEhPublico() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void swaggerEhPublico() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void demaisEndpointsDoActuatorExigemToken() throws Exception {
        mvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
    }
}
