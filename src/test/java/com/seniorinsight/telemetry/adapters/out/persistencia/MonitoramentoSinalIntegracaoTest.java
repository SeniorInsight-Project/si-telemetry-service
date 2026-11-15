package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.aparelho.MonitorarSinal;

import com.seniorinsight.telemetry.application.porta.saida.MonitoramentoSinalRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.application.porta.saida.RegistroSincronizacaoAparelho;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@TesteIntegracao
@Transactional
class MonitoramentoSinalIntegracaoTest {

    @Autowired RegistroSincronizacaoAparelho aparelhos;
    @Autowired MonitoramentoSinalRepositorio monitoramento;
    @Autowired PublicadorEventos eventos;
    @Autowired MonitorarSinal monitor;
    @Autowired EntityManager entidades;
    @Autowired JdbcTemplate jdbc;

    @Test
    void perdeUmaVezERearmaApenasComSincronizacaoPosterior() {
        UUID idosoId = UUID.randomUUID();

        Instant ultima = Instant.parse("2026-11-01T12:00:00Z");
        Instant perda = Instant.parse("2026-11-01T15:00:00Z");
        aparelhos.registrarStatus(idosoId, ultima, 62, null, null, null);
        monitor.verificar(perda.minusSeconds(1));
        assertThat(eventosDoIdoso(idosoId)).isEmpty();

        monitor.verificar(perda);
        monitor.verificar(perda.plusSeconds(900));
        assertThat(eventosDoIdoso(idosoId)).containsExactly("telemetry.signal.lost");

        aparelhos.registrarStatus(idosoId, perda.minusSeconds(60), 60, null, null, null);
        assertThat(eventosDoIdoso(idosoId)).containsExactly("telemetry.signal.lost");

        Instant retorno = perda.plusSeconds(1800);
        aparelhos.registrarStatus(idosoId, retorno, 58, null, null, null);
        aparelhos.registrarStatus(idosoId, retorno, 58, null, null, null);
        assertThat(eventosDoIdoso(idosoId)).containsExactly("telemetry.signal.lost", "telemetry.signal.recovered");

        monitor.verificar(retorno.plusSeconds(3 * 3600));
        assertThat(eventosDoIdoso(idosoId)).containsExactly("telemetry.signal.lost", "telemetry.signal.recovered",
                "telemetry.signal.lost");
    }

    @Test
    void adiaAlertaNoturnoAteAsSeteDaManha() {
        UUID idosoId = UUID.randomUUID();
        aparelhos.registrarStatus(idosoId, Instant.parse("2026-11-02T00:30:00Z"), null, null, null, null);
        monitor.verificar(Instant.parse("2026-11-02T03:30:00Z"));
        assertThat(eventosDoIdoso(idosoId)).isEmpty();

        monitor.verificar(Instant.parse("2026-11-02T10:00:00Z"));
        assertThat(eventosDoIdoso(idosoId)).containsExactly("telemetry.signal.lost");
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void duasReplicasPublicamUmaUnicaPerdaERetorno() throws Exception {
        UUID id = UUID.randomUUID();
        Instant ultima = Instant.parse("2026-11-01T12:00:00Z");
        Instant perda = ultima.plusSeconds(10800);
        aparelhos.registrar(id, ultima, 60);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var inicio = new java.util.concurrent.CountDownLatch(1);
            var a = executor.submit(() -> { inicio.await(); monitor.verificar(perda); return null; });
            var b = executor.submit(() -> { inicio.await(); monitor.verificar(perda); return null; });
            inicio.countDown();
            a.get(20, java.util.concurrent.TimeUnit.SECONDS);
            b.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE agregado_id = ? AND tipo_evento = 'telemetry.signal.lost'", Integer.class, id)).isEqualTo(1);
            Instant retorno = perda.plusSeconds(60);
            var c = executor.submit(() -> aparelhos.registrar(id, retorno, 60));
            var d = executor.submit(() -> aparelhos.registrar(id, retorno, 60));
            c.get(20, java.util.concurrent.TimeUnit.SECONDS);
            d.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE agregado_id = ? AND tipo_evento = 'telemetry.signal.recovered'", Integer.class, id)).isEqualTo(1);
        } finally {
            jdbc.update("DELETE FROM outbox_evento WHERE agregado_id = ?", id);
            jdbc.update("DELETE FROM status_aparelho WHERE idoso_id = ?", id);
        }
    }

    private List<String> eventosDoIdoso(UUID idosoId) {
        entidades.flush();
        return jdbc.queryForList("SELECT tipo_evento FROM outbox_evento WHERE agregado_id = ? ORDER BY criado_em",
                String.class, idosoId);
    }
}
