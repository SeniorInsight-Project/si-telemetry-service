package com.seniorinsight.telemetry.application.porta.saida;

import java.util.Optional;

import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;

public interface CacheMedicoesHoje {

    Optional<MedicoesHoje> buscar(String chave);

    void guardar(String chave, MedicoesHoje medicoes);
}
