package com.seniorinsight.telemetry.domain.medicao;

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
    OXIGENACAO("oxygen_saturation", "%", 50, 100);

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

    public double minimo() {
        return minimo;
    }

    public double maximo() {
        return maximo;
    }

    /** Monta a leitura deste tipo; a validação de faixa é feita por quem chama, com {@link #valorValido}. */
    public Leitura leitura(java.time.Instant medidaEm, double valor, String origem) {
        return new Leitura(codigo, medidaEm, valor, unidade, origem);
    }
}
