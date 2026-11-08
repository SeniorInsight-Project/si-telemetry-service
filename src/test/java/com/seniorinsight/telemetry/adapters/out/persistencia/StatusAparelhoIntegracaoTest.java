package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import com.seniorinsight.telemetry.application.porta.entrada.ReceberEvento;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TesteIntegracao
@Transactional
class StatusAparelhoIntegracaoTest {

    @Autowired StatusAparelhoRepositorio repositorio;
    @Autowired ReceberEvento receberEvento;

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
}
