package com.seniorinsight.telemetry.domain.medicao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.seniorinsight.telemetry.domain.aparelho.TipoDado;

public enum SerieTelemetria {
    PASSOS("steps", TipoDado.PASSOS, 0, Double.MAX_VALUE),
    BATIMENTO("heart_rate", TipoDado.BATIMENTOS, 25, 250),
    REPOUSO("resting_heart_rate", TipoDado.BATIMENTOS, 25, 250),
    OXIGENACAO("oxygen_saturation", TipoDado.OXIGENACAO, 50, 100),
    SONO("sleep", TipoDado.SONO, Math.nextUp(0.0), 86_400);

    private final String codigo;
    private final TipoDado consentimento;
    private final double minimo;
    private final double maximo;

    SerieTelemetria(String codigo, TipoDado consentimento, double minimo, double maximo) {
        this.codigo = codigo;
        this.consentimento = consentimento;
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public String codigo() { return codigo; }
    public double minimo() { return minimo; }
    public double maximo() { return maximo; }

    public static Set<String> autorizadas(Collection<TipoDado> consentimentos) {
        return Arrays.stream(values()).filter(serie -> consentimentos.contains(serie.consentimento))
                .map(SerieTelemetria::codigo).collect(Collectors.toUnmodifiableSet());
    }

    public static boolean valorValido(Leitura leitura) {
        return Arrays.stream(values()).anyMatch(serie -> serie.codigo.equals(leitura.tipo())
                && leitura.valor() >= serie.minimo && leitura.valor() <= serie.maximo);
    }
}
