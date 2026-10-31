package com.seniorinsight.telemetry.application.porta.saida;

import java.util.UUID;

/** Ids dos eventos já processados, para que a reentrega do broker não seja tratada duas vezes. */
public interface RegistroEventosRecebidos {

    /**
     * Registra o evento na transação corrente.
     *
     * @return {@code false} se o id já estava registrado (evento repetido)
     */
    boolean registrarSeNovo(UUID eventoId, String tipoEvento);
}
