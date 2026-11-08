package com.seniorinsight.telemetry.application.porta.saida;

import java.util.Optional;
import java.util.UUID;

public interface ExclusaoDadosRepositorio {

    boolean reservar(UUID protocolo);

    Optional<UUID> bloquearEBuscarUsuarioOpenWearables(UUID idosoId);

    void excluirEAnonimizar(UUID idosoId);
}
