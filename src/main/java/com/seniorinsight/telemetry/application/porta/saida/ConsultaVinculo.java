package com.seniorinsight.telemetry.application.porta.saida;

import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.acesso.Papel;

/** Vínculos usuário–idoso, mantidos pelo care-service (RF-007). */
public interface ConsultaVinculo {

    /**
     * Papel do usuário ({@code sub} do Keycloak) em relação ao idoso, ou vazio se não houver vínculo.
     *
     * @throws VinculoIndisponivelException se não foi possível consultar o care-service
     */
    Optional<Papel> papelDoUsuario(UUID idosoId, String sub);
}
