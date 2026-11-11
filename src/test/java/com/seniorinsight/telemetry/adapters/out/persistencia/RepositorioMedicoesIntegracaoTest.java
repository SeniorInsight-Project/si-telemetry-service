package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.porta.saida.RegistroSincronizacaoAparelho;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioMedicoes;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@TesteIntegracao
class RepositorioMedicoesIntegracaoTest {

    @Autowired RepositorioMedicoes medicoes;
    @Autowired RegistroSincronizacaoAparelho sincronizacao;
    @Autowired JdbcTemplate jdbc;

    private final UUID idoso = UUID.randomUUID();

    private Map<String, Object> agregado(String tipo) {
        return jdbc.queryForMap("SELECT * FROM agregado_diario WHERE idoso_id = ? AND tipo = ?", idoso, tipo);
    }

    @Test
    void gravaAmostrasESomaOResumoDoDia() {
        Instant manha = Instant.parse("2026-11-10T12:00:00Z");

        int novas = medicoes.gravarNovas(idoso, List.of(
                new Leitura("steps", manha, 100, "passos", "app"),
                new Leitura("steps", manha.plusSeconds(900), 250, "passos", "app"),
                new Leitura("heart_rate", manha, 70, "bpm", "app"),
                new Leitura("heart_rate", manha.plusSeconds(900), 90, "bpm", "app")));

        assertThat(novas).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM medicao WHERE idoso_id = ?", Integer.class, idoso))
                .isEqualTo(4);
        assertThat(agregado("steps")).containsEntry("soma", 350.0).containsEntry("quantidade", 2);
        Map<String, Object> bpm = agregado("heart_rate");
        assertThat(bpm).containsEntry("minimo", 70.0).containsEntry("maximo", 90.0)
                .containsEntry("media", 80.0).containsEntry("quantidade", 2);
    }

    @Test
    void reenviarAMesmaLeituraNaoDuplicaNemInflaOResumo() {
        Instant quando = Instant.parse("2026-11-10T12:00:00Z");
        Leitura passos = new Leitura("steps", quando, 100, "passos", "app");
        medicoes.gravarNovas(idoso, List.of(passos));

        int novas = medicoes.gravarNovas(idoso, List.of(passos, new Leitura("steps", quando.plusSeconds(60), 50, "passos", "app")));

        assertThat(novas).isEqualTo(1);
        assertThat(agregado("steps")).containsEntry("soma", 150.0).containsEntry("quantidade", 2);
    }

    @Test
    void origensDiferentesNoMesmoHorarioSaoAmostrasDistintas() {
        Instant quando = Instant.parse("2026-11-10T12:00:00Z");

        int novas = medicoes.gravarNovas(idoso, List.of(
                new Leitura("heart_rate", quando, 70, "bpm", "apple"),
                new Leitura("heart_rate", quando, 72, "bpm", "garmin")));

        assertThat(novas).isEqualTo(2);
    }

    @Test
    void diaDoResumoEhOdoFusoDeSaoPaulo() {
        // 02:00 UTC de 11/11 ainda é 10/11 (23:00) em São Paulo.
        medicoes.gravarNovas(idoso, List.of(new Leitura("steps", Instant.parse("2026-11-11T02:00:00Z"), 10, "passos", "app")));

        assertThat(agregado("steps").get("dia").toString()).isEqualTo("2026-11-10");
    }

    @Test
    void sincronizacaoSoAvancaEPreservaABateriaConhecida() {
        Instant t = Instant.parse("2026-11-10T12:00:00Z");
        sincronizacao.registrar(idoso, t, 80);
        sincronizacao.registrar(idoso, t.minusSeconds(3600), 10);
        sincronizacao.registrar(idoso, t.plusSeconds(60), null);

        Map<String, Object> status = jdbc.queryForMap("SELECT * FROM status_aparelho WHERE idoso_id = ?", idoso);
        assertThat(status).containsEntry("nivel_bateria", 80);
        assertThat(((java.sql.Timestamp) status.get("ultima_sincronizacao_em")).toInstant()).isEqualTo(t.plusSeconds(60));
    }

    @Test
    void novaSincronizacaoLimpaOSinalPerdido() {
        Instant t = Instant.parse("2026-11-10T12:00:00Z");
        sincronizacao.registrar(idoso, t, 50);
        jdbc.update("UPDATE status_aparelho SET sinal_perdido_desde = ? WHERE idoso_id = ?",
                java.sql.Timestamp.from(t), idoso);

        sincronizacao.registrar(idoso, t.plusSeconds(900), 49);

        assertThat(jdbc.queryForObject("SELECT sinal_perdido_desde FROM status_aparelho WHERE idoso_id = ?",
                java.sql.Timestamp.class, idoso)).isNull();
    }
}
