package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.MonitoramentoSinalRepositorio;
import com.seniorinsight.telemetry.domain.evento.SinalPerdido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class MonitoramentoSinalJdbc implements MonitoramentoSinalRepositorio {

    private final JdbcTemplate jdbc;

    MonitoramentoSinalJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<SinalPerdido> registrarPerdas(Instant limiteSincronizacao, Instant detectadoEm) {
        return jdbc.query("""
                UPDATE status_aparelho
                   SET sinal_perdido_desde = ?, atualizado_em = now()
                 WHERE sinal_perdido_desde IS NULL
                   AND ultima_sincronizacao_em <= ?
                RETURNING idoso_id, ultima_sincronizacao_em
                """, (rs, linha) -> new SinalPerdido(rs.getObject("idoso_id", UUID.class),
                        rs.getObject("ultima_sincronizacao_em", OffsetDateTime.class).toInstant(), detectadoEm),
                detectadoEm.atOffset(ZoneOffset.UTC), limiteSincronizacao.atOffset(ZoneOffset.UTC));
    }

    @Override
    public boolean registrarRetorno(UUID idosoId, Instant sincronizadoEm) {
        var instante = sincronizadoEm.atOffset(ZoneOffset.UTC);
        return jdbc.update("""
                UPDATE status_aparelho
                   SET sinal_perdido_desde = NULL, atualizado_em = now()
                 WHERE idoso_id = ? AND sinal_perdido_desde < ? AND ultima_sincronizacao_em = ?
                """, idosoId, instante, instante) == 1;
    }
}
