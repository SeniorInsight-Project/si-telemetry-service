package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.util.UUID;

/** Guarda em {@code status_aparelho} a última sincronização e a bateria vistas na ingestão (RF-020). */
public interface RegistroSincronizacaoAparelho {

    /**
     * Só avança: uma leitura mais antiga que a já registrada não muda nada.
     *
     * @param nivelBateria nível informado na leitura, ou {@code null}
     */
    void registrar(UUID idosoId, Instant sincronizadoEm, Integer nivelBateria);
}
