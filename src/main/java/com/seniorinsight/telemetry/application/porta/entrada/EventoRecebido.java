package com.seniorinsight.telemetry.application.porta.entrada;

import java.util.Objects;
import java.util.UUID;

/**
 * Evento de outro serviço recebido pela fila {@code telemetry.q}.
 *
 * @param id      {@code message_id} da mensagem (id da linha na outbox de quem publicou)
 * @param tipo    routing key, ex.: {@code consent.changed}
 * @param payload corpo JSON
 */
public record EventoRecebido(UUID id, String tipo, String payload) {

    public EventoRecebido {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(payload, "payload");
    }
}
