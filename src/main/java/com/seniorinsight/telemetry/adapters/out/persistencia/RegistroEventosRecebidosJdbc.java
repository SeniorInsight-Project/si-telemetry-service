package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.RegistroEventosRecebidos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Tabela {@code evento_recebido}: a chave primária no id do evento é o que garante a idempotência. */
@Repository
class RegistroEventosRecebidosJdbc implements RegistroEventosRecebidos {

    private final JdbcTemplate jdbc;

    RegistroEventosRecebidosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean registrarSeNovo(UUID eventoId, String tipoEvento) {
        return jdbc.update("""
                INSERT INTO evento_recebido (id, tipo_evento)
                VALUES (?, ?)
                ON CONFLICT (id) DO NOTHING
                """, eventoId, tipoEvento) == 1;
    }
}
