package com.seniorinsight.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** O contexto subir já prova que o Hibernate validou o mapeamento ({@code ddl-auto: validate}) contra a V1. */
@TesteIntegracao
class EstruturaBancoTest {

    private static final DateTimeFormatter SUFIXO_PARTICAO = DateTimeFormatter.ofPattern("yyyy_MM");

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayAplicaAV1NoSchemaTelemetry() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'telemetry'", String.class))
                .contains("medicao", "agregado_diario", "status_aparelho", "outbox_evento", "evento_recebido",
                        "flyway_schema_history");
    }

    @Test
    void medicaoEhParticionadaPorIntervaloDeMedidoEm() {
        String chave = jdbc.queryForObject(
                "SELECT pg_get_partkeydef('telemetry.medicao'::regclass)", String.class);

        assertThat(chave).isEqualTo("RANGE (medido_em)");
    }

    @Test
    void existemParticoesDoMesAtualEDosDoisSeguintesAlemDaPadrao() {
        LocalDate mes = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);

        assertThat(particoes()).contains(
                "medicao_padrao",
                "medicao_" + mes.format(SUFIXO_PARTICAO),
                "medicao_" + mes.plusMonths(1).format(SUFIXO_PARTICAO),
                "medicao_" + mes.plusMonths(2).format(SUFIXO_PARTICAO));
    }

    @Test
    void criarParticaoEhIdempotente() {
        LocalDate mes = LocalDate.of(2031, 7, 15);

        assertThat(jdbc.queryForObject("SELECT criar_particao_medicao(?)", Boolean.class, mes)).isTrue();
        assertThat(jdbc.queryForObject("SELECT criar_particao_medicao(?)", Boolean.class, mes)).isFalse();
        assertThat(particoes()).contains("medicao_2031_07");
    }

    @Test
    void medicaoVaiParaAParticaoDoMesEmUtc() {
        UUID idoso = UUID.randomUUID();
        LocalDate mes = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        inserirMedicao(idoso, mes.atStartOfDay().toInstant(ZoneOffset.UTC).toString());

        String particao = jdbc.queryForObject(
                "SELECT tableoid::regclass::text FROM medicao WHERE idoso_id = ?", String.class, idoso);

        assertThat(particao).isEqualTo("medicao_" + mes.format(SUFIXO_PARTICAO));
    }

    @Test
    void medicaoRepetidaPorIdosoTipoHorarioEOrigemEhRecusada() {
        UUID idoso = UUID.randomUUID();
        inserirMedicao(idoso, "2026-10-31T12:00:00Z");

        assertThatThrownBy(() -> inserirMedicao(idoso, "2026-10-31T12:00:00Z"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void outboxTemIndiceParcialDosPendentes() {
        String definicao = jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'telemetry' AND indexname = ?",
                String.class, "ix_outbox_evento_pendentes");

        assertThat(definicao).contains("(criado_em)").contains("WHERE (publicado_em IS NULL)");
    }

    private List<String> particoes() {
        return jdbc.queryForList("""
                SELECT c.relname
                  FROM pg_inherits i
                  JOIN pg_class c ON c.oid = i.inhrelid
                 WHERE i.inhparent = 'telemetry.medicao'::regclass
                """, String.class);
    }

    private void inserirMedicao(UUID idoso, String medidoEm) {
        jdbc.update("""
                INSERT INTO medicao (idoso_id, tipo, valor, unidade, medido_em, origem)
                VALUES (?, 'heart_rate', 72, 'bpm', ?::timestamptz, 'apple')
                """, idoso, medidoEm);
    }
}
