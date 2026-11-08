package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;

public interface ConsultaMedicoesHoje {

    MedicoesHoje buscar(UUID idosoId, LocalDate dia, Instant inicioDoDia, Instant agora,
                        boolean passos, boolean batimentos);
}
