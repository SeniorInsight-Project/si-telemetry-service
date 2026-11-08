package com.seniorinsight.telemetry.domain.evento;

import java.time.Instant;
import java.util.UUID;

public record SinalRecuperado(UUID idosoId, Instant sincronizadoEm) implements EventoDominio {

    @Override
    public String tipoAgregado() {
        return "status_aparelho";
    }

    @Override
    public UUID agregadoId() {
        return idosoId;
    }

    @Override
    public String tipoEvento() {
        return "telemetry.signal.recovered";
    }
}
