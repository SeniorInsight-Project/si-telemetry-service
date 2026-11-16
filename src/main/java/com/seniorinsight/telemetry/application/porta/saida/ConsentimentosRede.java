package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.consentimento.TipoDado;

/** Consentimento do idoso para a rede de proteção, projetado de {@code consent.changed}. */
public interface ConsentimentosRede {

    Set<TipoDado> tiposAutorizados(UUID idosoId);

    void bloquear(UUID idosoId);

    /** Grava o estado do tipo se {@code ocorridoEm} for mais novo que o gravado; no empate, a revogação vence. */
    void atualizar(UUID idosoId, TipoDado tipo, boolean autorizado, Instant ocorridoEm);
}
