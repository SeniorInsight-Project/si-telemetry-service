package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.domain.aparelho.StatusAparelho;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class StatusAparelhoJdbc implements StatusAparelhoRepositorio {

    private final JdbcTemplate jdbc;

    StatusAparelhoJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<StatusAparelho> buscar(UUID idosoId) {
        return jdbc.query("""
                SELECT s.ultima_sincronizacao_em, s.nivel_bateria, s.versao_app,
                       s.modelo_celular, s.sistema_operacional,
                       ARRAY(SELECT c.tipo FROM consentimento_rede c
                             WHERE c.idoso_id = s.idoso_id AND c.autorizado ORDER BY c.tipo) AS tipos_ativos
                  FROM status_aparelho s
                 WHERE s.idoso_id = ?
                """, (rs, linha) -> {
                    var sincronizacao = rs.getObject("ultima_sincronizacao_em", OffsetDateTime.class);
                    var tipos = rs.getArray("tipos_ativos");
                    try {
                        return new StatusAparelho(idosoId, sincronizacao == null ? null : sincronizacao.toInstant(),
                                rs.getObject("nivel_bateria", Integer.class), rs.getString("versao_app"),
                                rs.getString("modelo_celular"), rs.getString("sistema_operacional"),
                                Arrays.stream((String[]) tipos.getArray()).map(TipoDado::valueOf).toList());
                    } finally {
                        tipos.free();
                    }
                }, idosoId).stream().findFirst();
    }

    @Override
    public void registrar(UUID idosoId, Instant sincronizadoEm, Integer nivelBateria, String versaoApp,
                          String modeloCelular, String sistemaOperacional) {
        var instante = sincronizadoEm.atOffset(ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO status_aparelho (idoso_id, informado_em, ultima_sincronizacao_em, nivel_bateria,
                                             versao_app, modelo_celular, sistema_operacional)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (idoso_id) DO UPDATE
                    SET informado_em = EXCLUDED.informado_em,
                        ultima_sincronizacao_em = GREATEST(status_aparelho.ultima_sincronizacao_em,
                                                          EXCLUDED.ultima_sincronizacao_em),
                        nivel_bateria = EXCLUDED.nivel_bateria,
                        versao_app = EXCLUDED.versao_app,
                        modelo_celular = EXCLUDED.modelo_celular,
                        sistema_operacional = EXCLUDED.sistema_operacional,
                        atualizado_em = now()
                  WHERE status_aparelho.informado_em IS NULL
                     OR status_aparelho.informado_em < EXCLUDED.informado_em
                """, idosoId, instante, instante, nivelBateria, versaoApp, modeloCelular, sistemaOperacional);
    }

    @Override
    public void atualizarConsentimentoRede(UUID idosoId, TipoDado tipo, boolean autorizado, Instant ocorridoEm) {
        jdbc.update("""
                INSERT INTO consentimento_rede (idoso_id, tipo, autorizado, ocorrido_em)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (idoso_id, tipo) DO UPDATE
                    SET autorizado = EXCLUDED.autorizado, ocorrido_em = EXCLUDED.ocorrido_em
                  WHERE consentimento_rede.ocorrido_em < EXCLUDED.ocorrido_em
                     OR (consentimento_rede.ocorrido_em = EXCLUDED.ocorrido_em
                         AND consentimento_rede.autorizado AND NOT EXCLUDED.autorizado)
                """, idosoId, tipo.name(), autorizado, ocorridoEm.atOffset(ZoneOffset.UTC));
    }
}
