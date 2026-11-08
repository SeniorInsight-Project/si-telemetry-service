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
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;
import org.springframework.stereotype.Component;

@Component
class ProjetarConsentimentoRede implements TratadorEvento {

    private final ObjectReader leitor;
    private final StatusAparelhoRepositorio repositorio;

    ProjetarConsentimentoRede(ObjectMapper json, StatusAparelhoRepositorio repositorio) {
        this.leitor = json.readerFor(ConsentChanged.class).with(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
        this.repositorio = repositorio;
    }

    @Override
    public String tipoEvento() {
        return "consent.changed";
    }

    @Override
    public void tratar(EventoRecebido evento) {
        ConsentChanged consentimento;
        try {
            consentimento = leitor.readValue(evento.payload());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Payload inválido em consent.changed");
        }
        if (consentimento == null || !"1.0".equals(consentimento.specversion())
                || !"/care-service".equals(consentimento.source()) || !tipoEvento().equals(consentimento.type())
                || consentimento.time() == null || consentimento.data() == null) {
            throw new IllegalArgumentException("Envelope inválido em consent.changed");
        }
        Dados dados = consentimento.data();
        if (dados.idosoId() == null || dados.tipo() == null || dados.destinatario() == null
                || dados.autorizado() == null || !dados.autorizado().isBoolean()) {
            throw new IllegalArgumentException("Dados obrigatórios inválidos em consent.changed");
        }
        if (dados.destinatario() == Destinatario.REDE) {
            repositorio.atualizarConsentimentoRede(dados.idosoId(), dados.tipo(), dados.autorizado().booleanValue(),
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
