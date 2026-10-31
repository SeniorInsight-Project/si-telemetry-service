package com.seniorinsight.telemetry.application.evento;

import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;

/**
 * Trata um tipo de evento recebido (ex.: {@code consent.changed}). Cada tratador é um bean e roda dentro da
 * transação de {@link ReceberEventoService}.
 */
public interface TratadorEvento {

    /** Routing key tratada. */
    String tipoEvento();

    void tratar(EventoRecebido evento);
}
