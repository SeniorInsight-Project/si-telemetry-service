package com.seniorinsight.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.ConsultaVinculo;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioMedicoes;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * De ponta a ponta: o {@code consent.changed} chega pela fila, o banco e o Redis são reais, e só o care-service
 * (vínculo) é simulado, porque a rota {@code /internal/idosos/{id}/vinculo/{sub}} ainda não existe lá.
 */
@TesteIntegracao
class EstadoAtualIntegracaoTest {

    @Autowired MockMvc mvc;
    @Autowired RabbitTemplate rabbit;
    @Autowired RepositorioMedicoes medicoes;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean ConsultaVinculo vinculo;

    private final UUID idoso = UUID.randomUUID();

    private void consentimento(String tipo, boolean autorizado, Instant quando) {
        String corpo = """
                {"specversion":"1.0","id":"%s","source":"/care-service","type":"consent.changed","time":"%s",
                 "datacontenttype":"application/json","subject":"%s",
                 "data":{"idosoId":"%s","tipo":"%s","destinatario":"REDE","autorizado":%s}}"""
                .formatted(UUID.randomUUID(), quando, idoso, idoso, tipo, autorizado);
        UUID id = UUID.randomUUID();
        rabbit.send("", "telemetry.q", MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON).setMessageId(id.toString())
                .setType("consent.changed").build());
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM evento_recebido WHERE id = ?", Integer.class, id)).isEqualTo(1));
    }

    @Test
    void revogarBatimentosEscondeOValorNaProximaConsultaMesmoComCache() throws Exception {
        when(vinculo.papelDoUsuario(idoso, "familiar")).thenReturn(Optional.of(Papel.FAMILIAR_ADMINISTRADOR));
        Instant agora = Instant.now();
        medicoes.gravarNovas(idoso, List.of(
                new Leitura("steps", agora.minusSeconds(600), 1200, "passos", "app"),
                new Leitura("heart_rate", agora.minusSeconds(300), 81, "bpm", "app")));
        consentimento("PASSOS", true, agora.minusSeconds(100));
        consentimento("BATIMENTOS", true, agora.minusSeconds(100));
        String rota = "/v1/idosos/" + idoso + "/hoje";

        mvc.perform(get(rota).with(jwt().jwt(t -> t.subject("familiar")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.passosHoje").value(1200)).andExpect(jsonPath("$.frequenciaCardiaca").value(81));
        // segunda chamada: vem do cache
        mvc.perform(get(rota).with(jwt().jwt(t -> t.subject("familiar"))))
                .andExpect(jsonPath("$.frequenciaCardiaca").value(81));

        consentimento("BATIMENTOS", false, agora.minusSeconds(50));

        mvc.perform(get(rota).with(jwt().jwt(t -> t.subject("familiar")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.passosHoje").value(1200))
                .andExpect(jsonPath("$.frequenciaCardiaca").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.frequenciaCardiacaRepouso").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void quemNaoEstaNaRedeDoIdosoLeva403() throws Exception {
        when(vinculo.papelDoUsuario(idoso, "estranho")).thenReturn(Optional.empty());

        mvc.perform(get("/v1/idosos/" + idoso + "/hoje").with(jwt().jwt(t -> t.subject("estranho"))))
                .andExpect(status().isForbidden());
    }
}
