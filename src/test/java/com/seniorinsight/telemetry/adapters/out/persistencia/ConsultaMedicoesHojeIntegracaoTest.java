package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.porta.saida.ConsentimentosRede;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaMedicoesHoje;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioMedicoes;
import com.seniorinsight.telemetry.domain.consentimento.TipoDado;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@TesteIntegracao
class ConsultaMedicoesHojeIntegracaoTest {

    private static final LocalDate DIA = LocalDate.parse("2026-11-10");
    private static final Instant MANHA = Instant.parse("2026-11-10T12:00:00Z");
    private static final Instant AGORA = Instant.parse("2026-11-10T15:00:00Z");

    @Autowired ConsultaMedicoesHoje consulta;
    @Autowired RepositorioMedicoes medicoes;
    @Autowired ConsentimentosRede consentimentos;

    private final UUID idoso = UUID.randomUUID();

    @Test
    void somaOsPassosDoDiaEEscolheAFrequenciaMaisRecenteMesmoQuandoElaEMenor() {
        medicoes.gravarNovas(idoso, List.of(
                new Leitura("steps", MANHA, 100, "passos", "app"),
                new Leitura("steps", MANHA.plusSeconds(900), 250, "passos", "app"),
                new Leitura("heart_rate", MANHA, 95, "bpm", "app"),
                new Leitura("heart_rate", MANHA.plusSeconds(1800), 90, "bpm", "app"),
                new Leitura("resting_heart_rate", MANHA, 62, "bpm", "app")));

        MedicoesHoje hoje = consulta.buscar(idoso, DIA, AGORA, true, true);

        assertThat(hoje.passos()).isEqualTo(350);
        assertThat(hoje.frequenciaCardiaca()).isEqualTo(90);
        assertThat(hoje.frequenciaCardiacaRepouso()).isEqualTo(62);
        assertThat(hoje.coletadoEm()).isEqualTo(MANHA.plusSeconds(1800));
    }

    @Test
    void tipoNaoPedidoVemNuloENaoEntraNoHorario() {
        medicoes.gravarNovas(idoso, List.of(
                new Leitura("steps", MANHA, 100, "passos", "app"),
                new Leitura("heart_rate", MANHA.plusSeconds(3600), 80, "bpm", "app")));

        MedicoesHoje soPassos = consulta.buscar(idoso, DIA, AGORA, true, false);

        assertThat(soPassos.passos()).isEqualTo(100);
        assertThat(soPassos.frequenciaCardiaca()).isNull();
        assertThat(soPassos.coletadoEm()).isEqualTo(MANHA);

        MedicoesHoje soBatimentos = consulta.buscar(idoso, DIA, AGORA, false, true);

        assertThat(soBatimentos.passos()).isNull();
        assertThat(soBatimentos.frequenciaCardiaca()).isEqualTo(80);
        assertThat(soBatimentos.coletadoEm()).isEqualTo(MANHA.plusSeconds(3600));
    }

    @Test
    void passosAutorizadosSemLeituraHojeSaoZero() {
        MedicoesHoje hoje = consulta.buscar(idoso, DIA, AGORA, true, true);

        assertThat(hoje.passos()).isZero();
        assertThat(hoje.coletadoEm()).isNull();
    }

    @Test
    void ignoraLeiturasDeOutroIdosoEAmostrasNoFuturo() {
        medicoes.gravarNovas(UUID.randomUUID(), List.of(new Leitura("heart_rate", MANHA, 99, "bpm", "app")));
        medicoes.gravarNovas(idoso, List.of(
                new Leitura("heart_rate", MANHA, 70, "bpm", "app"),
                new Leitura("heart_rate", AGORA.plusSeconds(120), 130, "bpm", "app")));

        MedicoesHoje hoje = consulta.buscar(idoso, DIA, AGORA, false, true);

        assertThat(hoje.frequenciaCardiaca()).isEqualTo(70);
    }

    @Test
    void batimentoDeOntemAindaApareceDentroDaJanelaDeRecencia() {
        Instant ontem = Instant.parse("2026-11-09T22:00:00Z");
        medicoes.gravarNovas(idoso, List.of(new Leitura("heart_rate", ontem, 66, "bpm", "app")));

        MedicoesHoje hoje = consulta.buscar(idoso, DIA, AGORA, false, true);

        assertThat(hoje.frequenciaCardiaca()).isEqualTo(66);
        assertThat(hoje.coletadoEm()).isEqualTo(ontem);
    }

    @Test
    void consentimentoMaisNovoVence() {
        Instant t = Instant.parse("2026-11-08T15:30:00Z");
        consentimentos.atualizar(idoso, TipoDado.PASSOS, true, t);
        consentimentos.atualizar(idoso, TipoDado.PASSOS, false, t.plusSeconds(10));
        consentimentos.atualizar(idoso, TipoDado.PASSOS, true, t.plusSeconds(5)); // atrasado: ignorado

        assertThat(consentimentos.tiposAutorizados(idoso)).isEmpty();

        consentimentos.atualizar(idoso, TipoDado.BATIMENTOS, true, t);
        consentimentos.atualizar(idoso, TipoDado.BATIMENTOS, false, t); // empate: a revogação vence
        assertThat(consentimentos.tiposAutorizados(idoso)).isEmpty();

        consentimentos.atualizar(idoso, TipoDado.PASSOS, true, t.plusSeconds(20));
        assertThat(consentimentos.tiposAutorizados(idoso)).containsExactly(TipoDado.PASSOS);
    }
}
