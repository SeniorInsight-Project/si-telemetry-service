package com.seniorinsight.telemetry.adapters.in.mensageria;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.seniorinsight.telemetry.application.evento.TratadorEvento;
import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import com.seniorinsight.telemetry.application.porta.entrada.RegistrarConsentimentoRede;
import com.seniorinsight.telemetry.domain.consentimento.TipoDado;
import org.springframework.stereotype.Component;

/**
 * Trata {@code consent.changed} (si-contracts, {@code events/consent.changed.v1.json}). Só o destinatário
 * {@code REDE} interessa ao "hoje" e ao status do aparelho; MEDICO e OPERADORA são ignorados. Payload fora do
 * contrato lança {@link IllegalArgumentException}, e a mensagem vai para a dead-letter.
 */
@Component
class ProjetarConsentimentoRede implements TratadorEvento {

    static final String TIPO_EVENTO = "consent.changed";

    private final ObjectReader leitor;
    private final RegistrarConsentimentoRede registrar;

    ProjetarConsentimentoRede(ObjectMapper json, RegistrarConsentimentoRede registrar) {
        this.leitor = json.readerFor(ConsentChanged.class).with(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.registrar = registrar;
    }

    @Override
    public String tipoEvento() {
        return TIPO_EVENTO;
    }

    @Override
    public void tratar(EventoRecebido evento) {
        ConsentChanged consentimento;
        try {
            consentimento = leitor.readValue(evento.payload());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Payload inválido em consent.changed", e);
        }
        if (consentimento == null || !"1.0".equals(consentimento.specversion())
                || !"/care-service".equals(consentimento.source()) || !TIPO_EVENTO.equals(consentimento.type())
                || consentimento.time() == null || consentimento.data() == null) {
            throw new IllegalArgumentException("Envelope inválido em consent.changed");
        }
        Dados dados = consentimento.data();
        if (dados.idosoId() == null || dados.tipo() == null || dados.destinatario() == null
                || dados.autorizado() == null || !dados.autorizado().isBoolean()) {
            throw new IllegalArgumentException("Dados inválidos em consent.changed");
        }
        if (dados.destinatario() == Destinatario.REDE) {
            registrar.registrar(dados.idosoId(), dados.tipo(), dados.autorizado().booleanValue(),
                    consentimento.time());
        }
    }

    private record ConsentChanged(String specversion, String source, String type, Instant time, Dados data) {
    }

    private record Dados(UUID idosoId, TipoDado tipo, Destinatario destinatario, JsonNode autorizado) {
    }

    private enum Destinatario {
        REDE, MEDICO, OPERADORA
    }
}
