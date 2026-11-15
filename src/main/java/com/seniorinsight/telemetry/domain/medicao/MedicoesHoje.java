package com.seniorinsight.telemetry.domain.medicao;

import java.time.Instant;

/**
 * Últimos valores lidos para o resumo de hoje; {@code null} onde não há dado ou o tipo não foi consultado.
 *
 * @param coletadoEm horário da leitura mais recente entre os tipos consultados
 */
public record MedicoesHoje(Long passos, Integer frequenciaCardiaca, Integer frequenciaCardiacaRepouso,
                           Instant coletadoEm) {

    public static final MedicoesHoje VAZIO = new MedicoesHoje(null, null, null, null);
}
