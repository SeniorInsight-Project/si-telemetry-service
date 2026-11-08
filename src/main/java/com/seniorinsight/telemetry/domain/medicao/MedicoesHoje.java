package com.seniorinsight.telemetry.domain.medicao;

import java.time.Instant;

public record MedicoesHoje(Long passos, Integer frequenciaCardiaca, Integer frequenciaCardiacaRepouso, Instant coletadoEm) {
}
