package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaMedicoesHoje;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@TesteIntegracao
@Transactional
class ConsultaMedicoesHojeIntegracaoTest {

    private static final LocalDate DIA = LocalDate.parse("2026-11-01");
    private static final Instant INICIO = Instant.parse("2026-11-01T03:00:00Z");
    private static final Instant AGORA = Instant.parse("2026-11-01T17:30:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired ConsultaMedicoesHoje consulta;

    @Test
    void consultaAgregadoEUltimasLeiturasValidasApenasDosTiposAutorizados() {
        UUID idosoId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO agregado_diario (idoso_id, dia, tipo, unidade, soma)
                VALUES (?, ?, 'steps', 'count', 2340)
                """, idosoId, DIA);
        inserir(idosoId, "steps", 60, "2026-11-01T16:00:00Z");
        inserir(idosoId, "heart_rate", 80, "2026-11-01T15:00:00Z");
        inserir(idosoId, "heart_rate", 72, "2026-11-01T17:30:00Z");
        inserir(idosoId, "heart_rate", 251, "2026-11-01T17:29:59Z");
        inserir(idosoId, "heart_rate", 100, "2026-11-01T17:30:01Z");
        inserir(idosoId, "resting_heart_rate", 64, "2026-11-01T10:00:00Z");

        var todos = consulta.buscar(idosoId, DIA, INICIO, AGORA, true, true);
        assertThat(todos.passos()).isEqualTo(2340);
        assertThat(todos.frequenciaCardiaca()).isEqualTo(72);
        assertThat(todos.frequenciaCardiacaRepouso()).isEqualTo(64);
        assertThat(todos.coletadoEm()).isEqualTo(AGORA);

        var apenasPassos = consulta.buscar(idosoId, DIA, INICIO, AGORA, true, false);
        assertThat(apenasPassos.frequenciaCardiaca()).isNull();
        assertThat(apenasPassos.frequenciaCardiacaRepouso()).isNull();
        assertThat(apenasPassos.coletadoEm()).isEqualTo(Instant.parse("2026-11-01T16:00:00Z"));
    }

    @Test
    void naoDevolveBatimentoDeOntemComoSeFosseDeHoje() {
        UUID idosoId = UUID.randomUUID();
        inserir(idosoId, "heart_rate", 72, "2026-11-01T02:59:59Z");

        var estado = consulta.buscar(idosoId, DIA, INICIO, AGORA, true, true);

        assertThat(estado.passos()).isNull();
        assertThat(estado.frequenciaCardiaca()).isNull();
        assertThat(estado.coletadoEm()).isEqualTo(Instant.parse("2026-11-01T02:59:59Z"));
    }

    private void inserir(UUID idosoId, String tipo, int valor, String instante) {
        jdbc.update("""
                INSERT INTO medicao (idoso_id, tipo, valor, unidade, medido_em, origem)
                VALUES (?, ?, ?, 'count', ?::timestamptz, 'teste')
                """, idosoId, tipo, valor, instante);
    }
}
