package com.seniorinsight.telemetry.application.porta.entrada;

/**
 * Balanço de um envio.
 *
 * @param recebidas   leituras no envio
 * @param gravadas    amostras novas gravadas (depois da deduplicação)
 * @param descartadas amostras recusadas por faixa inválida ou horário no futuro
 */
public record ResultadoIngestao(int recebidas, int gravadas, int descartadas) {
}
