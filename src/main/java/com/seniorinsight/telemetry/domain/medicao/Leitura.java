package com.seniorinsight.telemetry.domain.medicao;

import java.time.Instant;
import java.util.Objects;

/**
 * Amostra de um sinal lida do wearable, ainda sem validação de faixa.
 *
 * @param tipo     tipo do sinal na origem, ex.: {@code heart_rate}, {@code steps}
 * @param medidaEm horário da amostra no aparelho
 * @param valor    valor medido
 * @param unidade  unidade do valor, ex.: {@code bpm}
 * @param origem   provedor que gerou a amostra; com idoso, tipo e horário identifica a leitura (RF-019)
 */
public record Leitura(String tipo, Instant medidaEm, double valor, String unidade, String origem) {

    public Leitura {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(medidaEm, "medidaEm");
        Objects.requireNonNull(unidade, "unidade");
        Objects.requireNonNull(origem, "origem");
    }
}
