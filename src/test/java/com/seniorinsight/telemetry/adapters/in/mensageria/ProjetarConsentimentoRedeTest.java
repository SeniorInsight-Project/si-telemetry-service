package com.seniorinsight.telemetry.adapters.in.mensageria;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;
import org.junit.jupiter.api.Test;

class ProjetarConsentimentoRedeTest {

    private static final UUID IDOSO = UUID.randomUUID();
    private static final Instant OCORRIDO_EM = Instant.parse("2026-11-01T13:15:00Z");
    private final StatusAparelhoRepositorio repositorio = mock(StatusAparelhoRepositorio.class);
    private final ProjetarConsentimentoRede projetar = new ProjetarConsentimentoRede(
            JsonMapper.builder().addModule(new JavaTimeModule()).build(), repositorio);

    @Test
    void projetaRevogacaoDeOxigenacaoParaRede() {
        projetar.tratar(evento("REDE", "false"));

        verify(repositorio).atualizarConsentimentoRede(IDOSO, TipoDado.OXIGENACAO, false, OCORRIDO_EM);
    }

    @Test
    void ignoraConsentimentoDeOutroDestinatario() {
        projetar.tratar(evento("MEDICO", "true"));

        verifyNoInteractions(repositorio);
    }

    @Test
    void rejeitaEventoSemDecisaoDeConsentimento() {
        assertThatThrownBy(() -> projetar.tratar(evento("REDE", "null")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> projetar.tratar(evento("REDE", "\"true\"")))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repositorio);
    }

    private EventoRecebido evento(String destinatario, String autorizado) {
        return new EventoRecebido(UUID.randomUUID(), "consent.changed", """
                {
                  "specversion":"1.0", "source":"/care-service", "type":"consent.changed", "time":"%s",
                  "data":{"idosoId":"%s","tipo":"OXIGENACAO","destinatario":"%s","autorizado":%s}
                }
                """.formatted(OCORRIDO_EM, IDOSO, destinatario, autorizado));
    }
}
