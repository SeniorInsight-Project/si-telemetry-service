package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.ExclusaoDadosRepositorio;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ExclusaoDadosJdbc implements ExclusaoDadosRepositorio {

    private final JdbcTemplate jdbc;

    ExclusaoDadosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean reservar(UUID protocolo) {
        return jdbc.update("INSERT INTO exclusao_confirmada (protocolo) VALUES (?) ON CONFLICT DO NOTHING",
                protocolo) == 1;
    }

    @Override
    public Optional<UUID> bloquearEBuscarUsuarioOpenWearables(UUID idosoId) {
        bloquear(idosoId.toString());
        var vinculo = jdbc.queryForList("SELECT usuario_id FROM usuario_open_wearables WHERE idoso_id = ?",
                UUID.class, idosoId).stream().findFirst();
        if (vinculo.isEmpty() && Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM medicao WHERE idoso_id = ?
                               UNION ALL SELECT 1 FROM agregado_diario WHERE idoso_id = ?)
                """, Boolean.class, idosoId, idosoId))) {
            throw new IllegalStateException("Exclusão aguardando a reconciliação do vínculo com o Open Wearables");
        }
        return vinculo;
    }

    @Override
    public void excluirEAnonimizar(UUID idosoId) {
        jdbc.update("INSERT INTO titular_excluido (chave) VALUES (sha256(convert_to(?, 'UTF8'))) ON CONFLICT DO NOTHING",
                idosoId.toString());
        jdbc.update("""
                WITH removidas AS (DELETE FROM medicao WHERE idoso_id = ? RETURNING tipo)
                INSERT INTO telemetria_anonima (conjunto, categoria, quantidade)
                SELECT 'MEDICAO', CASE WHEN tipo IN ('heart_rate', 'resting_heart_rate', 'steps', 'oxygen_saturation',
                                                     'sleep') THEN tipo ELSE 'outro' END AS categoria, count(*)
                  FROM removidas GROUP BY categoria
                ON CONFLICT (conjunto, categoria) DO UPDATE
                    SET quantidade = telemetria_anonima.quantidade + EXCLUDED.quantidade
                """, idosoId);
        jdbc.update("""
                WITH removidos AS (DELETE FROM agregado_diario WHERE idoso_id = ? RETURNING tipo)
                INSERT INTO telemetria_anonima (conjunto, categoria, quantidade)
                SELECT 'AGREGADO', CASE WHEN tipo IN ('heart_rate', 'resting_heart_rate', 'steps', 'oxygen_saturation',
                                                      'sleep') THEN tipo ELSE 'outro' END AS categoria, count(*)
                  FROM removidos GROUP BY categoria
                ON CONFLICT (conjunto, categoria) DO UPDATE
                    SET quantidade = telemetria_anonima.quantidade + EXCLUDED.quantidade
                """, idosoId);
        jdbc.update("DELETE FROM status_aparelho WHERE idoso_id = ?", idosoId);
        jdbc.update("DELETE FROM consentimento_rede WHERE idoso_id = ?", idosoId);
        jdbc.update("DELETE FROM usuario_open_wearables WHERE idoso_id = ?", idosoId);
        jdbc.update("""
                DELETE FROM outbox_evento WHERE agregado_id = ? OR payload->>'idosoId' = ?
                   OR payload->'data'->>'idosoId' = ?
                """, idosoId, idosoId.toString(), idosoId.toString());
    }

    private void bloquear(String chave) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))", rs -> null, chave);
    }
}
