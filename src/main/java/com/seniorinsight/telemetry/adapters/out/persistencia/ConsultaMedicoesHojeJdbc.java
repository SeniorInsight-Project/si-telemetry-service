package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.ConsultaMedicoesHoje;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Passos vêm do {@code agregado_diario} do dia; as frequências cardíacas, da última amostra de {@code medicao}
 * (pela chave única, um índice por idoso e tipo). Os tipos não pedidos nem são consultados.
 */
@Repository
class ConsultaMedicoesHojeJdbc implements ConsultaMedicoesHoje {

    private final JdbcClient jdbc;

    ConsultaMedicoesHojeJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MedicoesHoje buscar(UUID idosoId, LocalDate dia, Instant agora, boolean passos, boolean batimentos) {
        return jdbc.sql("""
                WITH ultimas AS (
                    SELECT sinais.tipo, amostra.valor, amostra.medido_em
                      FROM (VALUES ('steps', :passos), ('heart_rate', :batimentos),
                                   ('resting_heart_rate', :batimentos)) AS sinais(tipo, permitido)
                      CROSS JOIN LATERAL (
                          SELECT m.valor, m.medido_em
                            FROM medicao m
                           WHERE m.idoso_id = :idosoId AND m.tipo = sinais.tipo AND m.medido_em <= :agora
                           ORDER BY m.medido_em DESC, m.origem
                           LIMIT 1
                      ) amostra
                     WHERE sinais.permitido
                )
                SELECT CASE WHEN :passos
                            THEN COALESCE((SELECT CAST(a.soma AS BIGINT) FROM agregado_diario a
                                            WHERE a.idoso_id = :idosoId AND a.dia = :dia AND a.tipo = 'steps'), 0)
                       END AS passos_hoje,
                       MAX(CAST(valor AS INTEGER)) FILTER (WHERE tipo = 'heart_rate') AS frequencia_cardiaca,
                       MAX(CAST(valor AS INTEGER)) FILTER (WHERE tipo = 'resting_heart_rate') AS frequencia_repouso,
                       MAX(medido_em) AS coletado_em
                  FROM ultimas
                """)
                .param("idosoId", idosoId)
                .param("dia", dia)
                .param("agora", agora.atOffset(ZoneOffset.UTC))
                .param("passos", passos)
                .param("batimentos", batimentos)
                .query((rs, linha) -> {
                    OffsetDateTime coletadoEm = rs.getObject("coletado_em", OffsetDateTime.class);
                    return new MedicoesHoje(rs.getObject("passos_hoje", Long.class),
                            rs.getObject("frequencia_cardiaca", Integer.class),
                            rs.getObject("frequencia_repouso", Integer.class),
                            coletadoEm == null ? null : coletadoEm.toInstant());
                })
                .single();
    }
}
