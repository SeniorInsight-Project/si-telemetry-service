package com.seniorinsight.telemetry.application.porta.entrada;

import java.util.List;
import java.util.UUID;

/** Caso de uso: o app do idoso envia as leituras do relógio (RF-019). */
public interface IngerirTelemetria {

    /**
     * Valida, deduplica e grava as leituras, atualiza o resumo diário e o status do aparelho e publica as quedas.
     *
     * @param sub {@code sub} do token de quem enviou; precisa ser o próprio idoso
     * @throws com.seniorinsight.telemetry.application.acesso.AcessoNegadoException se não for o idoso
     */
    ResultadoIngestao ingerir(UUID idosoId, String sub, List<TelemetriaRecebida> leituras);
}
