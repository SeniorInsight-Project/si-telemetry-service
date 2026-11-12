package com.seniorinsight.telemetry.application.porta.saida;

import java.util.Optional;
import java.util.UUID;

public interface ExclusaoDadosRepositorio {

    boolean reservar(UUID protocolo);

    boolean confirmado(UUID protocolo);

    void bloquearParaExclusao(UUID idosoId);

    Optional<UUID> bloquearEBuscarUsuarioOpenWearables(UUID idosoId);

    void excluirEAnonimizar(UUID idosoId);
}
