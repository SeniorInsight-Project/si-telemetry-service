package com.seniorinsight.telemetry.domain.aparelho;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StatusAparelho(
        UUID idosoId,
        Instant ultimaSincronizacao,
        Integer nivelBateria,
        String versaoApp,
        String modeloCelular,
        String sistemaOperacional,
        List<TipoDado> tiposAtivos) {
}
