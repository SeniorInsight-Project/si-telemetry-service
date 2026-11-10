package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.RepositorioStatusAparelho;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class RepositorioStatusAparelhoJdbc implements RepositorioStatusAparelho {

    private final JdbcClient jdbc;

    RepositorioStatusAparelhoJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Registro> buscar(UUID idosoId) {
        return jdbc.sql("""
                SELECT ultima_sincronizacao_em, nivel_bateria, versao_app, modelo_celular, sistema_operacional
                  FROM status_aparelho WHERE idoso_id = :idosoId
                """)
                .param("idosoId", idosoId)
                .query((rs, linha) -> {
                    OffsetDateTime sincronizacao = rs.getObject("ultima_sincronizacao_em", OffsetDateTime.class);
                    return new Registro(sincronizacao == null ? null : sincronizacao.toInstant(),
                            rs.getObject("nivel_bateria", Integer.class), rs.getString("versao_app"),
                            rs.getString("modelo_celular"), rs.getString("sistema_operacional"));
                })
                .optional();
    }
}
