package com.seniorinsight.telemetry.application.medicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.AcessoNegadoException;
import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.entrada.ResultadoIngestao;
import com.seniorinsight.telemetry.application.porta.entrada.TelemetriaRecebida;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaVinculo;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.application.porta.saida.RegistroSincronizacaoAparelho;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioMedicoes;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.evento.QuedaDetectada;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IngerirTelemetriaServiceTest {

    private static final UUID IDOSO = UUID.randomUUID();
    private static final String SUB = "sub-do-idoso";
    private static final Instant AGORA = Instant.parse("2026-11-10T15:00:00Z");
    private static final Instant MEDIDA = Instant.parse("2026-11-10T14:30:00Z");

    ConsultaVinculo vinculo = mock(ConsultaVinculo.class);
    RepositorioMedicoes medicoes = mock(RepositorioMedicoes.class);
    RegistroSincronizacaoAparelho sincronizacao = mock(RegistroSincronizacaoAparelho.class);
    PublicadorEventos publicador = mock(PublicadorEventos.class);
    IngerirTelemetriaService servico;

    @BeforeEach
    void preparar() {
        when(vinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.of(Papel.IDOSO));
        when(medicoes.gravarNovas(eq(IDOSO), any())).thenAnswer(i -> ((List<?>) i.getArgument(1)).size());
        servico = new IngerirTelemetriaService(new VerificarAcessoIdoso(vinculo), medicoes, sincronizacao,
                publicador, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private static TelemetriaRecebida leitura(Instant medidaEm, Integer bpm, Integer passos, Integer bateria) {
        return new TelemetriaRecebida("XIAOMI_HEALTH", medidaEm, bpm, null, null, passos, null, null, null, null,
                null, null, null, bateria);
    }

    @SuppressWarnings("unchecked")
    private List<Leitura> gravadas() {
        ArgumentCaptor<List<Leitura>> captor = ArgumentCaptor.forClass(List.class);
        verify(medicoes).gravarNovas(eq(IDOSO), captor.capture());
        return captor.getValue();
    }

    @Test
    void geraUmaAmostraPorSinalComTipoEUnidadeDoContrato() {
        ResultadoIngestao resultado = servico.ingerir(IDOSO, SUB, List.of(leitura(MEDIDA, 78, 120, 80)));

        assertThat(gravadas()).containsExactlyInAnyOrder(
                new Leitura("heart_rate", MEDIDA, 78, "bpm", "XIAOMI_HEALTH"),
                new Leitura("steps", MEDIDA, 120, "passos", "XIAOMI_HEALTH"));
        assertThat(resultado).isEqualTo(new ResultadoIngestao(1, 2, 0));
    }

    @Test
    void descartaValorForaDaFaixaSemPerderOsDemaisSinais() {
        ResultadoIngestao resultado = servico.ingerir(IDOSO, SUB, List.of(leitura(MEDIDA, 400, 50, null)));

        assertThat(gravadas()).extracting(Leitura::tipo).containsExactly("steps");
        assertThat(resultado.descartadas()).isEqualTo(1);
    }

    @Test
    void descartaLeituraComHorarioNoFuturoMasTolera5Minutos() {
        Instant futuro = AGORA.plusSeconds(6 * 60);
        Instant quaseAgora = AGORA.plusSeconds(4 * 60);

        ResultadoIngestao resultado = servico.ingerir(IDOSO, SUB,
                List.of(leitura(futuro, 70, 10, 50), leitura(quaseAgora, 71, null, 50)));

        assertThat(gravadas()).extracting(Leitura::valor).containsExactly(71.0);
        assertThat(resultado).isEqualTo(new ResultadoIngestao(2, 1, 2));
    }

    @Test
    void registraSincronizacaoEBateriaDaLeituraMaisRecente() {
        servico.ingerir(IDOSO, SUB, List.of(
                leitura(MEDIDA, 70, null, 90), leitura(MEDIDA.plusSeconds(600), 71, null, 85),
                leitura(MEDIDA.minusSeconds(600), 69, null, 99)));

        verify(sincronizacao).registrar(IDOSO, MEDIDA.plusSeconds(600), 85);
    }

    @Test
    void bateriaForaDe0A100NaoEhRegistrada() {
        servico.ingerir(IDOSO, SUB, List.of(leitura(MEDIDA, 70, null, 150)));

        verify(sincronizacao).registrar(IDOSO, MEDIDA, null);
    }

    @Test
    void publicaEventoDeQuedaComOInstanteDoImpacto() {
        Instant impacto = MEDIDA.minusSeconds(20);
        var queda = new TelemetriaRecebida("XIAOMI_HEALTH", MEDIDA, null, null, null, null, null, null, null,
                true, 3.8, 0.92, impacto, 60);

        servico.ingerir(IDOSO, SUB, List.of(queda));

        verify(publicador).publicar(new QuedaDetectada(IDOSO, impacto, 3.8, 0.92));
    }

    @Test
    void quedaSemInstanteUsaOHorarioDaLeitura() {
        var queda = new TelemetriaRecebida(null, MEDIDA, null, null, null, null, null, null, null,
                true, null, null, null, null);

        servico.ingerir(IDOSO, SUB, List.of(queda));

        verify(publicador).publicar(new QuedaDetectada(IDOSO, MEDIDA, null, null));
    }

    @Test
    void naoPublicaQuandoNaoHouveQueda() {
        var sem = new TelemetriaRecebida(null, MEDIDA, 70, null, null, null, null, null, null,
                false, null, null, null, null);

        servico.ingerir(IDOSO, SUB, List.of(sem));

        verifyNoInteractions(publicador);
    }

    @Test
    void provedorAusenteViraOrigemApp() {
        servico.ingerir(IDOSO, SUB, List.of(new TelemetriaRecebida(" ", MEDIDA, 70, null, null, null, null, null,
                null, null, null, null, null, null)));

        assertThat(gravadas()).extracting(Leitura::origem).containsExactly("app");
    }

    @Test
    void soOProprioIdosoPodeEnviar() {
        when(vinculo.papelDoUsuario(IDOSO, "familiar")).thenReturn(Optional.of(Papel.FAMILIAR_ADMINISTRADOR));

        assertThatThrownBy(() -> servico.ingerir(IDOSO, "familiar", List.of(leitura(MEDIDA, 70, null, null))))
                .isInstanceOf(AcessoNegadoException.class);
        verify(medicoes, never()).gravarNovas(any(), any());
        verifyNoInteractions(sincronizacao, publicador);
    }
}
