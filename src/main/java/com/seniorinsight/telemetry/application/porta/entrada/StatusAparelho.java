package com.seniorinsight.telemetry.application.porta.entrada;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.consentimento.TipoDado;

/**
 * Schema {@code StatusAparelho} do contrato telemetry-v1. Campo que o aparelho ainda não informou vem {@code null};
 * {@code tiposAtivos} são os tipos que o idoso autoriza para a rede.
 */
public record StatusAparelho(UUID idosoId, Instant ultimaSincronizacao, Integer nivelBateria, String versaoApp,
                             String modeloCelular, String sistemaOperacional, List<TipoDado> tiposAtivos) {
}
