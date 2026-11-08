package com.seniorinsight.telemetry.application.acesso;

import java.util.UUID;

/** O usuário não tem vínculo com o idoso, ou o papel dele não dá acesso ao recurso (RF-007). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(UUID idosoId) {
        super("Sem acesso aos dados do idoso " + idosoId);
    }
}
