package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VinculosOpenWearables {

    List<UUID> idososAutorizados();

    Optional<Vinculo> buscar(UUID idosoId);

    Optional<Instant> inicioAutorizado(UUID idosoId);

    boolean temDadosLegados(UUID idosoId);

    void vincular(UUID idosoId, UUID usuarioId);

    void avancar(UUID idosoId, Instant fim);

    record Vinculo(UUID usuarioId, Instant coletadoAte) {
    }
}
