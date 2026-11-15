package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;

public interface ConsultaMedicoesHoje {

    /**
     * Soma de passos do {@code dia} e últimas frequências cardíacas até {@code agora}. Tipo com {@code false} não é
     * lido e vem {@code null}, e não entra em {@code coletadoEm}.
     */
    MedicoesHoje buscar(UUID idosoId, LocalDate dia, Instant agora, boolean passos, boolean batimentos);
}
