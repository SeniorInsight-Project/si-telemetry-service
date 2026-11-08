package com.seniorinsight.telemetry.application.lgpd;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.telemetry.application.evento.TratadorEvento;
import com.seniorinsight.telemetry.application.medicao.ReferenciaOpenWearables;
import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.ExclusaoDadosRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.domain.evento.ExclusaoConcluida;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TratarExclusao implements TratadorEvento {

    public static final String TIPO = "lgpd.erasure.requested";
    private static final String SCHEMA =
            "https://raw.githubusercontent.com/SeniorInsight-Project/si-contracts/main/events/lgpd.erasure.requested.v1.json";

    private final ObjectMapper json;
    private final ExclusaoDadosRepositorio repositorio;
    private final PublicadorEventos eventos;
    private final OpenWearables provedor;
    private final VinculosOpenWearables vinculos;
    private final ReferenciaOpenWearables referencias;

    public TratarExclusao(ObjectMapper json, ExclusaoDadosRepositorio repositorio, PublicadorEventos eventos,
                          OpenWearables provedor, VinculosOpenWearables vinculos,
                          ReferenciaOpenWearables referencias) {
        this.json = json;
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.provedor = provedor;
        this.vinculos = vinculos;
        this.referencias = referencias;
    }

    @Override
    public String tipoEvento() {
        return TIPO;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void tratar(EventoRecebido evento) {
        UUID mensagemId = evento.id();
        String corpo = evento.payload();
        JsonNode envelope;
        try {
            envelope = json.readTree(corpo);
        } catch (JsonProcessingException e) {
            throw invalido();
        }
        if (envelope == null || !envelope.isObject() || !"1.0".equals(envelope.path("specversion").asText())
                || !"/report-service".equals(envelope.path("source").asText())
                || !TIPO.equals(envelope.path("type").asText())
                || !SCHEMA.equals(envelope.path("dataschema").asText())
                || !"application/json".equals(envelope.path("datacontenttype").asText())
                || !mensagemId.equals(uuid(envelope.path("id")))) {
            throw invalido();
        }
        try {
            Instant.parse(envelope.path("time").asText());
        } catch (DateTimeParseException e) {
            throw invalido();
        }
        JsonNode data = envelope.path("data");
        UUID protocolo = uuid(data.path("protocolo"));
        UUID titular = uuid(data.path("titularId"));
        String tipo = data.path("tipoTitular").asText();
        String sub = data.path("titularSub").textValue();
        if (!protocolo.equals(uuid(envelope.path("subject"))) || sub == null || sub.isBlank() || sub.length() > 255
                || (!"IDOSO".equals(tipo) && !"FAMILIAR".equals(tipo))) {
            throw invalido();
        }

        if (!repositorio.reservar(protocolo)) {
            return;
        }
        if ("IDOSO".equals(tipo)) {
            var externo = repositorio.bloquearEBuscarUsuarioOpenWearables(titular)
                    .or(() -> referencias.gerar(titular).flatMap(provedor::buscarUsuarioPorReferencia));
            if (externo.isEmpty() && vinculos.temDadosLegados(titular)) {
                throw new IllegalStateException("Exclusão aguardando a reconciliação do vínculo com o Open Wearables");
            }
            externo.ifPresent(provedor::excluirUsuario);
            repositorio.excluirEAnonimizar(titular);
        }
        eventos.publicar(new ExclusaoConcluida(protocolo, "telemetry-service"));
    }

    private static UUID uuid(JsonNode campo) {
        if (!campo.isTextual()) {
            throw invalido();
        }
        try {
            String texto = campo.textValue();
            UUID valor = UUID.fromString(texto);
            if (valor.toString().equalsIgnoreCase(texto)) {
                return valor;
            }
        } catch (IllegalArgumentException e) {
            throw invalido();
        }
        throw invalido();
    }

    private static IllegalArgumentException invalido() {
        return new IllegalArgumentException("Pedido de exclusão inválido");
    }
}
