package com.seniorinsight.telemetry.adapters.in.mensageria;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import com.seniorinsight.telemetry.application.porta.entrada.RegistrarConsentimentoRede;
import com.seniorinsight.telemetry.domain.consentimento.TipoDado;
import org.junit.jupiter.api.Test;

/** Payload no formato de si-contracts/events/consent.changed.v1.json. */
class ProjetarConsentimentoRedeTest {

    private static final UUID IDOSO = UUID.fromString("44b69553-caa4-48a5-9e4b-22da0c4f2090");

    private final RegistrarConsentimentoRede registrar = mock(RegistrarConsentimentoRede.class);
    private final ProjetarConsentimentoRede tratador = new ProjetarConsentimentoRede(
            JsonMapper.builder().findAndAddModules().build(), registrar);

    private static EventoRecebido evento(String destinatario, String autorizado) {
        return evento("1.0", destinatario, autorizado);
    }

    private static EventoRecebido evento(String specversion, String destinatario, String autorizado) {
        return new EventoRecebido(UUID.randomUUID(), "consent.changed", """
                {"specversion":"%s","id":"176f6ee8-3920-4ea4-b558-b50ee934825a","source":"/care-service",
                 "type":"consent.changed","time":"2026-11-08T15:30:00Z","datacontenttype":"application/json",
                 "subject":"%s","data":{"idosoId":"%s","tipo":"PASSOS","destinatario":"%s","autorizado":%s}}"""
                .formatted(specversion, IDOSO, IDOSO, destinatario, autorizado));
    }

    @Test
    void revogacaoParaARedeEhProjetada() {
        tratador.tratar(evento("REDE", "false"));

        verify(registrar).registrar(IDOSO, TipoDado.PASSOS, false, Instant.parse("2026-11-08T15:30:00Z"));
    }

    @Test
    void autorizacaoParaARedeEhProjetada() {
        tratador.tratar(evento("REDE", "true"));

        verify(registrar).registrar(IDOSO, TipoDado.PASSOS, true, Instant.parse("2026-11-08T15:30:00Z"));
    }

    @Test
    void medicoEOperadoraSaoIgnorados() {
        tratador.tratar(evento("MEDICO", "true"));
        tratador.tratar(evento("OPERADORA", "false"));

        verifyNoInteractions(registrar);
    }

    @Test
    void payloadForaDoContratoVaiFalhar() {
        assertThatThrownBy(() -> tratador.tratar(evento("2.0", "REDE", "true")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tratador.tratar(evento("REDE", "\"true\"")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tratador.tratar(evento("TODOS", "true")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tratador.tratar(new EventoRecebido(UUID.randomUUID(), "consent.changed", "{}")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tratador.tratar(new EventoRecebido(UUID.randomUUID(), "consent.changed", "nao é json")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(registrar, org.mockito.Mockito.never()).registrar(any(), any(), anyBoolean(), any());
    }
}
