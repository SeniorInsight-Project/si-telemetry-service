package com.seniorinsight.telemetry.adapters.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.aparelho.StatusAparelhoService;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaVinculo;
import com.seniorinsight.telemetry.application.porta.saida.MonitoramentoSinalRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.aparelho.StatusAparelho;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StatusAparelhoController.class,
        properties = "KEYCLOAK_ISSUER_URI=http://keycloak.invalid/realms/seniorinsight")
@Import({SegurancaConfig.class, StatusAparelhoService.class, VerificarAcessoIdoso.class})
class StatusAparelhoControllerTest {

    private static final UUID IDOSO = UUID.fromString("56e67608-4907-4b24-b13d-15a52a58ae46");
    private static final String ROTA = "/v1/idosos/" + IDOSO + "/status";
    private static final String SUB = "usuario";
    private static final Instant SINCRONIZADO_EM = Instant.parse("2026-11-01T13:15:00Z");

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean ConsultaVinculo consultaVinculo;
    @MockitoBean StatusAparelhoRepositorio repositorio;
    @MockitoBean MonitoramentoSinalRepositorio monitoramento;
    @MockitoBean PublicadorEventos eventos;

    @Test
    void familiarVinculadoRecebeOsCamposDoContrato() throws Exception {
        when(consultaVinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.of(Papel.FAMILIAR_APOIO));
        when(repositorio.buscar(IDOSO)).thenReturn(Optional.of(new StatusAparelho(IDOSO, SINCRONIZADO_EM,
                62, "1.0.4", "Galaxy A15", "Android 14", List.of(TipoDado.PASSOS))));

        mvc.perform(get(ROTA).with(jwt().jwt(token -> token.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idosoId").value(IDOSO.toString()))
                .andExpect(jsonPath("$.ultimaSincronizacao").value("2026-11-01T13:15:00Z"))
                .andExpect(jsonPath("$.nivelBateria").value(62))
                .andExpect(jsonPath("$.versaoApp").value("1.0.4"))
                .andExpect(jsonPath("$.modeloCelular").value("Galaxy A15"))
                .andExpect(jsonPath("$.sistemaOperacional").value("Android 14"))
                .andExpect(jsonPath("$.tiposAtivos.length()").value(1))
                .andExpect(jsonPath("$.tiposAtivos[0]").value("PASSOS"));
    }

    @Test
    void exigeTokenMesmoComCabecalhosDeIdentidade() throws Exception {
        mvc.perform(get(ROTA).header("X-Authenticated-User-Id", SUB))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(consultaVinculo, repositorio);
    }

    @Test
    void negaAcessoSemVinculo() throws Exception {
        when(consultaVinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.empty());

        mvc.perform(get(ROTA).with(jwt().jwt(token -> token.subject(SUB))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(repositorio);
    }

    @Test
    void retorna404QuandoAindaNaoHaRegistroDoAparelho() throws Exception {
        when(consultaVinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.of(Papel.IDOSO));
        when(repositorio.buscar(IDOSO)).thenReturn(Optional.empty());

        mvc.perform(get(ROTA).with(jwt().jwt(token -> token.subject(SUB))))
                .andExpect(status().isNotFound());
    }

    @Test
    void idosoPodeRegistrarSincronizacaoComMetadadosDoCelular() throws Exception {
        when(consultaVinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.of(Papel.IDOSO));

        mvc.perform(post("/v1/aparelhos/status").with(jwt().jwt(token -> token.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idosoId":"%s","sincronizadoEm":"%s","nivelBateria":62,
                                 "versaoApp":"1.0.4","modeloCelular":"Galaxy A15","sistemaOperacional":"Android 14"}
                                """.formatted(IDOSO, SINCRONIZADO_EM)))
                .andExpect(status().isNoContent());

        verify(repositorio).registrar(IDOSO, SINCRONIZADO_EM, 62, "1.0.4", "Galaxy A15", "Android 14");
    }

    @Test
    void familiarNaoPodeRegistrarSincronizacaoDoIdoso() throws Exception {
        when(consultaVinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.of(Papel.FAMILIAR_ADMINISTRADOR));

        mvc.perform(post("/v1/aparelhos/status").with(jwt().jwt(token -> token.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idosoId":"%s","sincronizadoEm":"%s","nivelBateria":62}
                                """.formatted(IDOSO, SINCRONIZADO_EM)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(repositorio);
    }
}
