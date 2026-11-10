package com.seniorinsight.telemetry.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.AcessoNegadoException;
import com.seniorinsight.telemetry.application.porta.entrada.IngerirTelemetria;
import com.seniorinsight.telemetry.application.porta.entrada.ResultadoIngestao;
import com.seniorinsight.telemetry.application.porta.entrada.TelemetriaRecebida;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TelemetriaController.class,
        properties = "KEYCLOAK_ISSUER_URI=http://keycloak.invalid/realms/seniorinsight")
@Import({SegurancaConfig.class, TratamentoErros.class})
class TelemetriaControllerTest {

    private static final UUID IDOSO = UUID.fromString("56e67608-4907-4b24-b13d-15a52a58ae46");
    // O gateway entrega /api/v1/telemetry/idosos/{id}/telemetria como /v1/idosos/{id}/telemetria.
    private static final String ROTA = "/v1/idosos/" + IDOSO + "/telemetria";
    private static final String LEITURA = """
            {"provider":"XIAOMI_HEALTH","timestamp":"2026-11-10T14:30:00Z",
             "heart_rate":{"bpm":78,"resting_bpm":64,"hrv_ms":42.5},
             "steps":{"count":120,"distance_meters":90.5,"calories_kcal":4.2},
             "fall_detection":{"fall_detected":true,"impact_force_g":3.8,"confidence":0.92,"fall_timestamp":"2026-11-10T14:29:40Z"},
             "battery_level":85,"spo2_percentage":97.5}""";

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean IngerirTelemetria ingestao;

    @Test
    void leituraUnicaConverteOPayloadSnakeCaseEResponde202ComOBalanco() throws Exception {
        when(ingestao.ingerir(eq(IDOSO), eq("sub"), any())).thenReturn(new ResultadoIngestao(1, 6, 0));

        mvc.perform(post(ROTA).with(jwt().jwt(t -> t.subject("sub")))
                        .contentType(MediaType.APPLICATION_JSON).content(LEITURA))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.recebidas").value(1))
                .andExpect(jsonPath("$.gravadas").value(6))
                .andExpect(jsonPath("$.descartadas").value(0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TelemetriaRecebida>> captor = ArgumentCaptor.forClass(List.class);
        verify(ingestao).ingerir(eq(IDOSO), eq("sub"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue()).containsExactly(new TelemetriaRecebida(
                "XIAOMI_HEALTH", Instant.parse("2026-11-10T14:30:00Z"), 78, 64, 42.5, 120, 90.5, 4.2, 97.5,
                true, 3.8, 0.92, Instant.parse("2026-11-10T14:29:40Z"), 85));
    }

    @Test
    void loteAceitaVariasLeituras() throws Exception {
        when(ingestao.ingerir(eq(IDOSO), eq("sub"), any())).thenReturn(new ResultadoIngestao(2, 3, 1));

        mvc.perform(post(ROTA + "/lote").with(jwt().jwt(t -> t.subject("sub")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"readings\":[" + LEITURA + "," + LEITURA + "]}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.recebidas").value(2));
    }

    @Test
    void loteVazioEhRecusado() throws Exception {
        mvc.perform(post(ROTA + "/lote").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"readings\":[]}"))
                .andExpect(status().isBadRequest());
        verify(ingestao, never()).ingerir(any(), any(), any());
    }

    @Test
    void loteComMaisDe500LeiturasEhRecusado() throws Exception {
        String leituras = String.join(",", java.util.Collections.nCopies(501, LEITURA));

        mvc.perform(post(ROTA + "/lote").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"readings\":[" + leituras + "]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leituraSemHorarioEhRecusada() throws Exception {
        mvc.perform(post(ROTA).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heart_rate\":{\"bpm\":70}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quemNaoEhOIdosoRecebe403() throws Exception {
        when(ingestao.ingerir(eq(IDOSO), eq("sub"), any())).thenThrow(new AcessoNegadoException(IDOSO));

        mvc.perform(post(ROTA).with(jwt().jwt(t -> t.subject("sub")))
                        .contentType(MediaType.APPLICATION_JSON).content(LEITURA))
                .andExpect(status().isForbidden());
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(LEITURA))
                .andExpect(status().isUnauthorized());
    }
}
