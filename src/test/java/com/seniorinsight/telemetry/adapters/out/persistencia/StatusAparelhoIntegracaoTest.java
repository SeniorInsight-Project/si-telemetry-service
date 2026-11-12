package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.medicao.ColetarMedicoesAutorizadas;
import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import com.seniorinsight.telemetry.application.porta.entrada.ReceberEvento;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.RegistroMedicoes;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

@TesteIntegracao
@Transactional
class StatusAparelhoIntegracaoTest {

    @Autowired StatusAparelhoRepositorio repositorio;
    @Autowired ReceberEvento receberEvento;
    @Autowired RegistroMedicoes registro;
    @Autowired TransactionOperations transacoes;
    @Autowired JdbcTemplate jdbc;

    @Test
    void preservaUltimaSincronizacaoEAplicaApenasConsentimentosDaRedeEmOrdem() {
        UUID idosoId = UUID.randomUUID();
        Instant sincronizadoEm = Instant.parse("2026-11-01T13:15:00Z");

        enviar(idosoId, "PASSOS", "REDE", true, sincronizadoEm);
        enviar(idosoId, "OXIGENACAO", "REDE", true, sincronizadoEm);
        repositorio.registrar(idosoId, sincronizadoEm, 62, "1.0.4", "Galaxy A15", "Android 14");
        repositorio.registrar(idosoId, sincronizadoEm.minusSeconds(60), 10, "0.9", "Outro", "Android 13");

        var inicial = repositorio.buscar(idosoId).orElseThrow();
        assertThat(inicial.ultimaSincronizacao()).isEqualTo(sincronizadoEm);
        assertThat(inicial.nivelBateria()).isEqualTo(62);
        assertThat(inicial.modeloCelular()).isEqualTo("Galaxy A15");
        assertThat(inicial.sistemaOperacional()).isEqualTo("Android 14");
        assertThat(inicial.tiposAtivos()).containsExactlyInAnyOrder(TipoDado.OXIGENACAO, TipoDado.PASSOS);

        enviar(idosoId, "OXIGENACAO", "REDE", false, sincronizadoEm.plusSeconds(60));
        enviar(idosoId, "OXIGENACAO", "REDE", true, sincronizadoEm.plusSeconds(30));
        enviar(idosoId, "OXIGENACAO", "MEDICO", true, sincronizadoEm.plusSeconds(90));

        assertThat(repositorio.buscar(idosoId).orElseThrow().tiposAtivos()).containsExactly(TipoDado.PASSOS);
    }

    private void enviar(UUID idosoId, String tipo, String destinatario, boolean autorizado, Instant ocorridoEm) {
        receberEvento.receber(new EventoRecebido(UUID.randomUUID(), "consent.changed", """
                {
                  "specversion":"1.0", "source":"/care-service", "type":"consent.changed", "time":"%s",
                  "data":{"idosoId":"%s","tipo":"%s","destinatario":"%s","autorizado":%b}
                }
                """.formatted(ocorridoEm, idosoId, tipo, destinatario, autorizado)));
    }

    @Test
    void revogacaoDuranteBuscaImpedeInsertEDeduplicaTipoAutorizado() {
        UUID idosoId = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        Instant inicio = Instant.parse("2026-11-01T13:15:00Z");
        Instant fim = inicio.plusSeconds(60);
        enviar(idosoId, "PASSOS", "REDE", true, inicio);
        enviar(idosoId, "BATIMENTOS", "REDE", true, inicio);
        OpenWearables provedor = mock(OpenWearables.class);
        var coleta = new ColetarMedicoesAutorizadas(repositorio, provedor, registro, transacoes);
        var passos = new Leitura("steps", inicio, 150, "count", "teste");
        var batimentos = new Leitura("heart_rate", inicio, 72, "bpm", "teste");
        when(provedor.buscarLeituras(usuario, inicio, fim, Set.of("steps", "heart_rate", "resting_heart_rate")))
                .thenAnswer(chamada -> {
                    enviar(idosoId, "PASSOS", "REDE", false, fim);
                    return List.of(passos, batimentos, batimentos);
                });

        coleta.coletar(idosoId, usuario, inicio, fim);

        assertThat(jdbc.queryForList("SELECT tipo FROM medicao WHERE idoso_id = ?", String.class, idosoId))
                .containsExactly("heart_rate");
    }
}
