package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Leitura de {@code status_aparelho}. */
public interface RepositorioStatusAparelho {

    Optional<Registro> buscar(UUID idosoId);

    record Registro(Instant ultimaSincronizacao, Integer nivelBateria, String versaoApp, String modeloCelular,
                    String sistemaOperacional) {
    }
}
