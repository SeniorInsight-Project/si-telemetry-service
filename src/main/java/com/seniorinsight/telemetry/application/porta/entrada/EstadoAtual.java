package com.seniorinsight.telemetry.application.porta.entrada;

import java.time.Instant;
import java.util.UUID;

/**
 * Schema {@code EstadoAtual} do contrato telemetry-v1. Tipo não autorizado, ou sem leitura nas últimas 48 h (RN-003),
 * vem {@code null}.
 */
public record EstadoAtual(UUID idosoId, Long passosHoje, int metaPassos, Integer frequenciaCardiaca,
                          Integer frequenciaCardiacaRepouso, Instant coletadoEm) {
}
