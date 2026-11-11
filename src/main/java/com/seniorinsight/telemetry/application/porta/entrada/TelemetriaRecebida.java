package com.seniorinsight.telemetry.application.porta.entrada;

import java.time.Instant;

/**
 * Leitura consolidada enviada pelo app do idoso (formato herdado do care-service). Todo campo de sinal é opcional:
 * cada leitura pode trazer só alguns sinais.
 */
public record TelemetriaRecebida(
        String provedor,
        Instant medidaEm,
        Integer batimentos,
        Integer batimentosRepouso,
        Double variabilidadeCardiacaMs,
        Integer passos,
        Double distanciaMetros,
        Double caloriasAtivasKcal,
        Double oxigenacaoPercentual,
        Boolean quedaDetectada,
        Double forcaImpactoG,
        Double confiancaQueda,
        Instant quedaEm,
        Integer nivelBateria) {
}
