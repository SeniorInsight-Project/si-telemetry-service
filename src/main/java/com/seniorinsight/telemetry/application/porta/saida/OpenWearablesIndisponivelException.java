package com.seniorinsight.telemetry.application.porta.saida;

/** O Open Wearables não respondeu (rede, timeout, 5xx) ou o circuit breaker está aberto. Vale tentar depois. */
public class OpenWearablesIndisponivelException extends RuntimeException {

    public OpenWearablesIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
