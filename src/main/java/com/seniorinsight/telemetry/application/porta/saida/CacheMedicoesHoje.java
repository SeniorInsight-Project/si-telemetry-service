package com.seniorinsight.telemetry.application.porta.saida;

import java.util.Optional;

import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;

/** Cache do resumo de hoje (RNF-002). Indisponível = miss: quem usa cai no banco. */
public interface CacheMedicoesHoje {

    Optional<MedicoesHoje> buscar(String chave);

    void guardar(String chave, MedicoesHoje medicoes);
}
