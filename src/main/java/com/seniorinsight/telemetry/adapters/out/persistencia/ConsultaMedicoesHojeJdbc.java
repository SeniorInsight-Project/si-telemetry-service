package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.ConsultaMedicoesHoje;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import com.seniorinsight.telemetry.domain.medicao.SerieTelemetria;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class ConsultaMedicoesHojeJdbc implements ConsultaMedicoesHoje {

    private final JdbcClient jdbc;

    ConsultaMedicoesHojeJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MedicoesHoje buscar(UUID idosoId, LocalDate dia, Instant inicioDoDia, Instant agora,
                               boolean passos, boolean batimentos) {
        return jdbc.sql("""
                WITH ultimas AS (
                    SELECT sinais.tipo, amostra.valor, amostra.medido_em
                      FROM (VALUES (:seriePassos, :passos), (:serieBatimento, :batimentos),
                                   (:serieRepouso, :batimentos)) AS sinais(tipo, permitido)
                      CROSS JOIN LATERAL (
                          SELECT m.valor, m.medido_em
                            FROM medicao m
                           WHERE m.idoso_id = :idosoId AND m.tipo = sinais.tipo AND m.medido_em <= :agora
                             AND (sinais.tipo = :seriePassos OR m.valor BETWEEN :batimentoMin AND :batimentoMax)
                           ORDER BY m.medido_em DESC, m.origem
                           LIMIT 1
                      ) amostra
                     WHERE sinais.permitido
                )
                SELECT (SELECT CAST(a.soma AS BIGINT) FROM agregado_diario a
                         WHERE :passos AND a.idoso_id = :idosoId AND a.dia = :dia AND a.tipo = :seriePassos
                           AND a.soma >= 0) AS passos_hoje,
                       (SELECT CAST(valor AS INTEGER) FROM ultimas
                         WHERE tipo = :serieBatimento AND medido_em >= :inicioDoDia) AS frequencia_cardiaca,
                       (SELECT CAST(valor AS INTEGER) FROM ultimas
                         WHERE tipo = :serieRepouso AND medido_em >= :inicioDoDia) AS frequencia_repouso,
                       (SELECT MAX(medido_em) FROM ultimas) AS coletado_em
                """)
                .param("idosoId", idosoId)
                .param("dia", dia)
                .param("inicioDoDia", inicioDoDia.atOffset(ZoneOffset.UTC))
                .param("agora", agora.atOffset(ZoneOffset.UTC))
                .param("passos", passos)
                .param("batimentos", batimentos)
                .param("seriePassos", SerieTelemetria.PASSOS.codigo())
                .param("serieBatimento", SerieTelemetria.BATIMENTO.codigo())
                .param("serieRepouso", SerieTelemetria.REPOUSO.codigo())
                .param("batimentoMin", SerieTelemetria.BATIMENTO.minimo())
                .param("batimentoMax", SerieTelemetria.BATIMENTO.maximo())
                .query((rs, linha) -> {
                    var coletadoEm = rs.getObject("coletado_em", OffsetDateTime.class);
                    return new MedicoesHoje(rs.getObject("passos_hoje", Long.class),
                            rs.getObject("frequencia_cardiaca", Integer.class),
                            rs.getObject("frequencia_repouso", Integer.class),
                            coletadoEm == null ? null : coletadoEm.toInstant());
                })
                .single();
    }
}
