package com.seniorinsight.telemetry.domain.evento;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TelemetriaIngerida(UUID idosoId, List<Agregado> agregados) implements EventoDominio {

    @Override public String tipoAgregado() { return "agregado_diario"; }
    @Override public UUID agregadoId() { return idosoId; }
    @Override public String tipoEvento() { return "telemetry.ingested"; }

    public record Agregado(LocalDate dia, String tipo, String unidade, int quantidade, double minimo,
                          double maximo, double media, double soma, Double linhaBase14Dias, int horasComDados) {
    }
}
