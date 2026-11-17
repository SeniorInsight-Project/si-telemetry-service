package com.seniorinsight.telemetry.application.medicao;

import java.util.UUID;

/** O SDK só pode ser inicializado enquanto houver consentimento ativo para algum dado. */
public class ConsentimentoNaoAtivoException extends RuntimeException {

    public ConsentimentoNaoAtivoException(UUID idosoId) {
        super("Nenhum consentimento ativo para o idoso " + idosoId);
    }
}
