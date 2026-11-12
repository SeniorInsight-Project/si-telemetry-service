package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.RegistroMedicoes;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
class RegistroMedicoesJdbc implements RegistroMedicoes {

    private final JdbcTemplate jdbc;

    RegistroMedicoesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(UUID idosoId, List<Leitura> leituras) {
        List<Object[]> parametros = leituras.stream().map(leitura -> new Object[] {
                idosoId, leitura.tipo(), leitura.valor(), leitura.unidade(),
                leitura.medidaEm().atOffset(ZoneOffset.UTC), leitura.origem()
        }).toList();
        jdbc.batchUpdate("""
                INSERT INTO medicao (idoso_id, tipo, valor, unidade, medido_em, origem)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (idoso_id, tipo, medido_em, origem) DO NOTHING
                """, parametros);
    }
}
