package com.seniorinsight.telemetry.domain.medicao;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.seniorinsight.telemetry.domain.consentimento.TipoDado;

/**
 * Tipos de sinal gravados em {@code medicao} e {@code agregado_diario}, com unidade e faixa fisiologicamente válida.
 * Os nomes de tipo são os lidos pela consulta de "hoje" (HU-030/031).
 */
public enum TipoMedicao {

    PASSOS("steps", "passos", 0, 200_000),
    DISTANCIA("distance", "m", 0, 500_000),
    CALORIAS_ATIVAS("active_calories", "kcal", 0, 20_000),
    FREQUENCIA_CARDIACA("heart_rate", "bpm", 30, 260),
    FREQUENCIA_CARDIACA_REPOUSO("resting_heart_rate", "bpm", 30, 200),
    VARIABILIDADE_CARDIACA("hrv", "ms", 0, 500),
    OXIGENACAO("oxygen_saturation", "%", 50, 100),
    SONO("sleep", "s", Math.nextUp(0.0), 86_400);

    private final String codigo;
    private final String unidade;
    private final double minimo;
    private final double maximo;

    TipoMedicao(String codigo, String unidade, double minimo, double maximo) {
        this.codigo = codigo;
        this.unidade = unidade;
        this.minimo = minimo;
        this.maximo = maximo;
    }

    /** Valor gravado na coluna {@code tipo}. */
    public String codigo() {
        return codigo;
    }

    public String unidade() {
        return unidade;
    }

    public boolean valorValido(double valor) {
        return Double.isFinite(valor) && valor >= minimo && valor <= maximo;
    }

    public static boolean leituraValida(Leitura leitura) {
        return Arrays.stream(values()).anyMatch(tipo -> tipo.codigo.equals(leitura.tipo())
                && tipo.valorValido(leitura.valor()));
    }

    public static Set<String> seriesWearables(Set<TipoDado> consentidos) {
        return Arrays.stream(values()).filter(tipo -> switch (tipo) {
            case PASSOS -> consentidos.contains(TipoDado.PASSOS);
            case FREQUENCIA_CARDIACA, FREQUENCIA_CARDIACA_REPOUSO -> consentidos.contains(TipoDado.BATIMENTOS);
            case OXIGENACAO -> consentidos.contains(TipoDado.OXIGENACAO);
            case SONO -> consentidos.contains(TipoDado.SONO);
            default -> false;
        }).map(TipoMedicao::codigo).collect(Collectors.toUnmodifiableSet());
    }

    /** Monta a leitura deste tipo; a validação de faixa é feita por quem chama, com {@link #valorValido}. */
    public Leitura leitura(java.time.Instant medidaEm, double valor, String origem) {
        return new Leitura(codigo, medidaEm, valor, unidade, origem);
    }
}
