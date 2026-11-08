package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class VinculosOpenWearablesJdbc implements VinculosOpenWearables {

    private static final String AUTORIZADOS = """
            autorizado AND tipo IN ('BATIMENTOS', 'PASSOS', 'OXIGENACAO', 'SONO')
            """;

    private final JdbcTemplate jdbc;

    VinculosOpenWearablesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UUID> idososAutorizados() {
        return jdbc.queryForList("SELECT DISTINCT idoso_id FROM consentimento_rede WHERE " + AUTORIZADOS, UUID.class);
    }

    @Override
    public Optional<Vinculo> buscar(UUID idosoId) {
        return jdbc.query("""
                SELECT usuario_id, coletado_ate FROM usuario_open_wearables WHERE idoso_id = ?
                """, (rs, linha) -> {
                    var cursor = rs.getObject("coletado_ate", OffsetDateTime.class);
                    return new Vinculo(rs.getObject("usuario_id", UUID.class), cursor == null ? null : cursor.toInstant());
                }, idosoId).stream().findFirst();
    }

    @Override
    public Optional<Instant> inicioAutorizado(UUID idosoId) {
        return Optional.ofNullable(jdbc.queryForObject(
                "SELECT MAX(ocorrido_em) FROM consentimento_rede WHERE idoso_id = ? AND " + AUTORIZADOS,
                OffsetDateTime.class, idosoId)).map(OffsetDateTime::toInstant);
    }

    @Override
    public boolean temDadosLegados(UUID idosoId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM medicao WHERE idoso_id = ?
                               UNION ALL SELECT 1 FROM agregado_diario WHERE idoso_id = ?)
                """, Boolean.class, idosoId, idosoId));
    }

    @Override
    public void vincular(UUID idosoId, UUID usuarioId) {
        jdbc.update("INSERT INTO usuario_open_wearables (idoso_id, usuario_id) VALUES (?, ?)", idosoId, usuarioId);
    }

    @Override
    public void avancar(UUID idosoId, Instant fim) {
        jdbc.update("""
                UPDATE usuario_open_wearables SET coletado_ate = GREATEST(coletado_ate, ?)
                 WHERE idoso_id = ?
                """, fim.atOffset(ZoneOffset.UTC), idosoId);
    }
}
