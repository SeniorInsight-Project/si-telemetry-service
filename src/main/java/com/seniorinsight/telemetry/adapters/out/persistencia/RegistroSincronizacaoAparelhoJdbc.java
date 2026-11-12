package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.RegistroSincronizacaoAparelho;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Upsert em {@code status_aparelho}: a sincronização e a bateria só avançam para leituras mais novas, e uma leitura
 * sem bateria não apaga a última conhecida. Os demais campos (versão do app, modelo) pertencem ao registro de status
 * feito pelo app e não são tocados aqui.
 */
@Repository
class RegistroSincronizacaoAparelhoJdbc implements RegistroSincronizacaoAparelho {

    private final JdbcClient jdbc;

    RegistroSincronizacaoAparelhoJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(UUID idosoId, Instant sincronizadoEm, Integer nivelBateria) {
        jdbc.sql("""
                INSERT INTO status_aparelho AS s (idoso_id, nivel_bateria, ultima_sincronizacao_em)
                VALUES (:idosoId, :bateria, :sincronizadoEm)
                ON CONFLICT (idoso_id) DO UPDATE
                    SET ultima_sincronizacao_em = EXCLUDED.ultima_sincronizacao_em,
                        nivel_bateria = COALESCE(EXCLUDED.nivel_bateria, s.nivel_bateria),
                        sinal_perdido_desde = NULL,
                        atualizado_em = now()
                  WHERE s.ultima_sincronizacao_em IS NULL
                     OR s.ultima_sincronizacao_em < EXCLUDED.ultima_sincronizacao_em
                """)
                .param("idosoId", idosoId)
                .param("bateria", nivelBateria)
                .param("sincronizadoEm", sincronizadoEm.atOffset(ZoneOffset.UTC))
                .update();
    }
}
