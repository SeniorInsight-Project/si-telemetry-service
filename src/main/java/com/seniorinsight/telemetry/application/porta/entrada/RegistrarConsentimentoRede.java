package com.seniorinsight.telemetry.application.porta.entrada;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.consentimento.TipoDado;

/** Projeta no telemetry o consentimento do idoso para a rede de proteção, vindo de {@code consent.changed}. */
public interface RegistrarConsentimentoRede {

    /** Só vale se for mais novo que o já registrado para o idoso e o tipo. */
    void registrar(UUID idosoId, TipoDado tipo, boolean autorizado, Instant ocorridoEm);
}
