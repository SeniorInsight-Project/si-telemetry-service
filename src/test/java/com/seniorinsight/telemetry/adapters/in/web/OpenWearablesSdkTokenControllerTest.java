package com.seniorinsight.telemetry.adapters.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.seniorinsight.telemetry.application.medicao.ConsentimentoNaoAtivoException;
import com.seniorinsight.telemetry.application.medicao.EmitirTokenSdkOpenWearables;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OpenWearablesSdkTokenController.class,
        properties = "KEYCLOAK_ISSUER_URI=http://keycloak.invalid/realms/seniorinsight")
@Import({SegurancaConfig.class, TratamentoErros.class})
class OpenWearablesSdkTokenControllerTest {

    private static final UUID IDOSO = UUID.fromString("56e67608-4907-4b24-b13d-15a52a58ae46");
    private static final String ROTA = "/v1/idosos/" + IDOSO + "/open-wearables/sdk-token";

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean EmitirTokenSdkOpenWearables emitir;

    @Test
    void devolveAsCredenciaisEmCamelCase() throws Exception {
        when(emitir.emitir(IDOSO, "sub")).thenReturn(new EmitirTokenSdkOpenWearables.Resposta(
                "https://open-wearables.example", UUID.fromString("0b8f3c52-7d1e-4a3b-9c1d-2e5f6a7b8c9d"),
                "access", "refresh"));

        mvc.perform(post(ROTA).with(jwt().jwt(t -> t.subject("sub"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("https://open-wearables.example"))
                .andExpect(jsonPath("$.userId").value("0b8f3c52-7d1e-4a3b-9c1d-2e5f6a7b8c9d"))
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    void semConsentimentoDa403() throws Exception {
        when(emitir.emitir(IDOSO, "sub")).thenThrow(new ConsentimentoNaoAtivoException(IDOSO));

        mvc.perform(post(ROTA).with(jwt().jwt(t -> t.subject("sub"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void semTokenDa401() throws Exception {
        mvc.perform(post(ROTA)).andExpect(status().isUnauthorized());
    }
}
