package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.ConsentimentosRede;
import com.seniorinsight.telemetry.domain.consentimento.TipoDado;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class ConsentimentosRedeJdbc implements ConsentimentosRede {

    private final JdbcClient jdbc;

    ConsentimentosRedeJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<TipoDado> tiposAutorizados(UUID idosoId) {
        Set<TipoDado> tipos = EnumSet.noneOf(TipoDado.class);
        jdbc.sql("SELECT tipo FROM consentimento_rede WHERE idoso_id = :idosoId AND autorizado")
                .param("idosoId", idosoId)
                .query((rs, linha) -> TipoDado.valueOf(rs.getString("tipo")))
                .list()
                .forEach(tipos::add);
        return tipos;
    }

    @Override
    public void atualizar(UUID idosoId, TipoDado tipo, boolean autorizado, Instant ocorridoEm) {
        jdbc.sql("""
                INSERT INTO consentimento_rede AS c (idoso_id, tipo, autorizado, ocorrido_em)
                VALUES (:idosoId, :tipo, :autorizado, :ocorridoEm)
                ON CONFLICT (idoso_id, tipo) DO UPDATE
                    SET autorizado = EXCLUDED.autorizado, ocorrido_em = EXCLUDED.ocorrido_em
                  WHERE c.ocorrido_em < EXCLUDED.ocorrido_em
                     OR (c.ocorrido_em = EXCLUDED.ocorrido_em AND c.autorizado AND NOT EXCLUDED.autorizado)
                """)
                .param("idosoId", idosoId)
                .param("tipo", tipo.name())
                .param("autorizado", autorizado)
                .param("ocorridoEm", ocorridoEm.atOffset(ZoneOffset.UTC))
                .update();
    }
}
