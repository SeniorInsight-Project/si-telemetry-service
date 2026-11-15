package com.seniorinsight.telemetry.application.porta.saida;

import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.medicao.Leitura;

/** Séries do idoso ({@code medicao}) e seu resumo diário ({@code agregado_diario}). */
public interface RepositorioMedicoes {

    /**
     * Grava na transação corrente as leituras que ainda não existem (idoso, tipo, horário e origem iguais já
     * gravados são ignorados, RF-019) e soma só as novas ao resumo do dia.
     *
     * @return quantas amostras novas foram gravadas
     */
    int gravarNovas(UUID idosoId, List<Leitura> leituras);
}
