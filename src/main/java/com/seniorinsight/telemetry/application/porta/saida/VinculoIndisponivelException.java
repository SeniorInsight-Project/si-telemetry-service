package com.seniorinsight.telemetry.application.porta.saida;

/** O care-service não respondeu à consulta de vínculo; o acesso não pode ser decidido. */
public class VinculoIndisponivelException extends RuntimeException {

    public VinculoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
