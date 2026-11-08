package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.aparelho.MonitorarSinal;
import com.seniorinsight.telemetry.application.aparelho.StatusAparelhoService;
import com.seniorinsight.telemetry.application.porta.saida.MonitoramentoSinalRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@TesteIntegracao
@Transactional
class MonitoramentoSinalIntegracaoTest {

    @Autowired StatusAparelhoRepositorio aparelhos;
    @Autowired MonitoramentoSinalRepositorio monitoramento;
    @Autowired PublicadorEventos eventos;
    @Autowired MonitorarSinal monitor;
    @Autowired EntityManager entidades;
    @Autowired JdbcTemplate jdbc;

    @Test
    void perdeUmaVezERearmaApenasComSincronizacaoPosterior() {
        UUID idosoId = UUID.randomUUID();
        var acesso = new VerificarAcessoIdoso((id, sub) -> Optional.of(Papel.IDOSO));
        var status = new StatusAparelhoService(acesso, aparelhos, monitoramento, eventos);
        Instant ultima = Instant.parse("2026-11-01T12:00:00Z");
        Instant perda = Instant.parse("2026-11-01T15:00:00Z");
        status.registrar(idosoId, "idoso", ultima, 62, null, null, null);
        monitor.verificar(perda.minusSeconds(1));
        assertThat(eventosDoIdoso(idosoId)).isEmpty();

        monitor.verificar(perda);
        monitor.verificar(perda.plusSeconds(900));
        assertThat(eventosDoIdoso(idosoId)).containsExactly("telemetry.signal.lost");

        status.registrar(idosoId, "idoso", perda.minusSeconds(60), 60, null, null, null);
        assertThat(eventosDoIdoso(idosoId)).containsExactly("telemetry.signal.lost");

        Instant retorno = perda.plusSeconds(1800);
        status.registrar(idosoId, "idoso", retorno, 58, null, null, null);
        status.registrar(idosoId, "idoso", retorno, 58, null, null, null);
        assertThat(eventosDoIdoso(idosoId)).containsExactly("telemetry.signal.lost", "telemetry.signal.recovered");

        monitor.verificar(retorno.plusSeconds(3 * 3600));
        assertThat(eventosDoIdoso(idosoId)).containsExactly("telemetry.signal.lost", "telemetry.signal.recovered",
                "telemetry.signal.lost");
    }

    @Test
    void adiaAlertaNoturnoAteAsSeteDaManha() {
        UUID idosoId = UUID.randomUUID();
        aparelhos.registrar(idosoId, Instant.parse("2026-11-02T00:30:00Z"), null, null, null, null);
        monitor.verificar(Instant.parse("2026-11-02T03:30:00Z"));
        assertThat(eventosDoIdoso(idosoId)).isEmpty();

        monitor.verificar(Instant.parse("2026-11-02T10:00:00Z"));
        assertThat(eventosDoIdoso(idosoId)).containsExactly("telemetry.signal.lost");
    }

    private List<String> eventosDoIdoso(UUID idosoId) {
        entidades.flush();
        return jdbc.queryForList("SELECT tipo_evento FROM outbox_evento WHERE agregado_id = ? ORDER BY criado_em",
                String.class, idosoId);
    }
}
