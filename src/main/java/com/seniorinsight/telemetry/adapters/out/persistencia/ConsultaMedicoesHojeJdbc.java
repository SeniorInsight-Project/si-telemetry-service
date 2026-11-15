package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.ConsultaMedicoesHoje;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import com.seniorinsight.telemetry.domain.medicao.TipoMedicao;
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
                      FROM (VALUES (:seriePassos, :passos, :minPassos, :maxPassos),
                                   (:serieBatimento, :batimentos, :minBatimento, :maxBatimento),
                                   (:serieRepouso, :batimentos, :minRepouso, :maxRepouso))
                           AS sinais(tipo, permitido, minimo, maximo)
                      CROSS JOIN LATERAL (
                          SELECT m.valor, m.medido_em
                            FROM medicao m
                           WHERE m.idoso_id = :idosoId AND m.tipo = sinais.tipo AND m.medido_em <= :agora
                             AND m.valor BETWEEN sinais.minimo AND sinais.maximo
                           ORDER BY m.medido_em DESC, m.origem
                           LIMIT 1
                      ) amostra
                     WHERE sinais.permitido
                )
                SELECT CASE WHEN :passos
                            THEN COALESCE((SELECT CAST(a.soma AS BIGINT) FROM agregado_diario a
                                            WHERE a.idoso_id = :idosoId AND a.dia = :dia AND a.tipo = :seriePassos), 0)
                       END AS passos_hoje,
                       (SELECT CAST(valor AS INTEGER) FROM ultimas WHERE tipo = :serieBatimento) AS frequencia_cardiaca,
                       (SELECT CAST(valor AS INTEGER) FROM ultimas WHERE tipo = :serieRepouso) AS frequencia_repouso,
                       (SELECT MAX(medido_em) FROM ultimas) AS coletado_em
                """)
                .param("idosoId", idosoId)
                .param("dia", dia)
                .param("agora", agora.atOffset(ZoneOffset.UTC))
                .param("passos", passos)
                .param("batimentos", batimentos)
                .param("seriePassos", TipoMedicao.PASSOS.codigo())
                .param("minPassos", TipoMedicao.PASSOS.minimo())
                .param("maxPassos", TipoMedicao.PASSOS.maximo())
                .param("serieBatimento", TipoMedicao.FREQUENCIA_CARDIACA.codigo())
                .param("minBatimento", TipoMedicao.FREQUENCIA_CARDIACA.minimo())
                .param("maxBatimento", TipoMedicao.FREQUENCIA_CARDIACA.maximo())
                .param("serieRepouso", TipoMedicao.FREQUENCIA_CARDIACA_REPOUSO.codigo())
                .param("minRepouso", TipoMedicao.FREQUENCIA_CARDIACA_REPOUSO.minimo())
                .param("maxRepouso", TipoMedicao.FREQUENCIA_CARDIACA_REPOUSO.maximo())
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
