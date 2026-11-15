package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.porta.saida.RegistroMedicoes;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@TesteIntegracao
@Transactional
class RegistroColetaIntegracaoTest {
    @Autowired RegistroMedicoes registro;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entidades;

    @Test
    void agregaUmaVezEPublicaCoberturaHoraria() {
        UUID idoso = UUID.randomUUID();
        var leitura = new Leitura("heart_rate", Instant.parse("2026-11-12T15:00:00Z"), 72, "bpm", "teste");
        registro.registrar(idoso, List.of(leitura, leitura));
        entidades.flush();
        assertThat(jdbc.queryForObject("""
                SELECT quantidade FROM agregado_diario WHERE idoso_id = ? AND tipo = 'heart_rate'
                """, Integer.class, idoso)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT payload #>> '{agregados,0,horasComDados}' FROM outbox_evento
                 WHERE agregado_id = ? AND tipo_evento = 'telemetry.ingested'
                """, String.class, idoso)).isEqualTo("1");
    }
}
