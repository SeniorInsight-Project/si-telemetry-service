package com.seniorinsight.telemetry.application.medicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.AcessoNegadoException;
import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.saida.CacheMedicoesHoje;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaMedicoesHoje;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaVinculo;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.junit.jupiter.api.Test;

class ConsultarEstadoAtualTest {

    private static final UUID IDOSO = UUID.randomUUID();
    private static final String SUB = "usuario";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Instant AGORA = Instant.parse("2026-11-02T02:58:00Z");

    private final ConsultaVinculo vinculo = mock(ConsultaVinculo.class);
    private final StatusAparelhoRepositorio consentimentos = mock(StatusAparelhoRepositorio.class);
    private final ConsultaMedicoesHoje medicoes = mock(ConsultaMedicoesHoje.class);
    private final CacheMedicoesHoje cache = mock(CacheMedicoesHoje.class);

    @Test
    void devolveAgregadoDoDiaELeiturasRecentes() {
        vincular(Papel.IDOSO, TipoDado.PASSOS, TipoDado.BATIMENTOS);
        var dados = new MedicoesHoje(2340L, 72, 64, AGORA.minusSeconds(60));
        when(medicoes.buscar(IDOSO, LocalDate.parse("2026-11-01"), Instant.parse("2026-11-01T03:00:00Z"),
                AGORA, true, true)).thenReturn(dados);

        var resposta = consultarEm(AGORA);

        assertThat(resposta.passosHoje()).isEqualTo(2340);
        assertThat(resposta.metaPassos()).isEqualTo(4000);
        assertThat(resposta.frequenciaCardiaca()).isEqualTo(72);
        assertThat(resposta.frequenciaCardiacaRepouso()).isEqualTo(64);
        assertThat(resposta.coletadoEm()).isEqualTo(dados.coletadoEm());
        verify(cache).guardar(chave("2026-11-01", true, true), dados);
    }

    @Test
    void revogacaoMudaAChaveEImpedeReutilizarBatimentosAnteriores() {
        vincular(Papel.FAMILIAR_APOIO, TipoDado.PASSOS);
        when(cache.buscar(chave("2026-11-01", true, false)))
                .thenReturn(Optional.of(new MedicoesHoje(2340L, 72, 64, AGORA)));

        var resposta = consultarEm(AGORA);

        verify(cache).buscar(chave("2026-11-01", true, false));
        verifyNoInteractions(medicoes);
        assertThat(resposta.passosHoje()).isEqualTo(2340);
        assertThat(resposta.frequenciaCardiaca()).isNull();
        assertThat(resposta.frequenciaCardiacaRepouso()).isNull();
    }

    @Test
    void viraODiaPeloFusoLocalMesmoComCacheDeOntemValido() {
        vincular(Papel.FAMILIAR_ADMINISTRADOR, TipoDado.PASSOS);
        Instant meiaNoite = Instant.parse("2026-11-02T03:00:00Z");
        var dados = new MedicoesHoje(null, null, null, AGORA);
        when(medicoes.buscar(IDOSO, LocalDate.parse("2026-11-02"), meiaNoite, meiaNoite, true, false))
                .thenReturn(dados);

        assertThat(consultarEm(meiaNoite).passosHoje()).isNull();
        verify(cache).buscar(chave("2026-11-02", true, false));
    }

    @Test
    void dadosComMaisDe48HorasNaoVoltamNemQuandoEstaoNoCache() {
        vincular(Papel.FAMILIAR_APOIO, TipoDado.PASSOS, TipoDado.BATIMENTOS);
        Instant antiga = AGORA.minusSeconds(48 * 3600 + 1);
        when(cache.buscar(chave("2026-11-01", true, true)))
                .thenReturn(Optional.of(new MedicoesHoje(2340L, 72, 64, antiga)));

        var resposta = consultarEm(AGORA);

        assertThat(resposta.passosHoje()).isNull();
        assertThat(resposta.frequenciaCardiaca()).isNull();
        assertThat(resposta.frequenciaCardiacaRepouso()).isNull();
        assertThat(resposta.coletadoEm()).isEqualTo(antiga);
    }

    @Test
    void semConsentimentoNaoLeMedicoesNemCache() {
        vincular(Papel.FAMILIAR_APOIO, TipoDado.SONO);

        var resposta = consultarEm(AGORA);

        assertThat(resposta.coletadoEm()).isNull();
        assertThat(resposta.passosHoje()).isNull();
        verifyNoInteractions(cache, medicoes);
    }

    @Test
    void semVinculoNaoConsultaNemConsentimento() {
        when(vinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultarEm(AGORA)).isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(cache, medicoes, consentimentos);
    }

    private void vincular(Papel papel, TipoDado... tipos) {
        when(vinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.of(papel));
        when(consentimentos.buscarTiposAtivosRede(IDOSO)).thenReturn(List.of(tipos));
    }

    private ConsultarEstadoAtual.EstadoAtual consultarEm(Instant agora) {
        var consulta = new ConsultarEstadoAtual(new VerificarAcessoIdoso(vinculo), consentimentos, medicoes, cache,
                Clock.fixed(agora, ZoneOffset.UTC), FUSO.getId(), 4000);
        return consulta.consultar(IDOSO, SUB);
    }

    private String chave(String dia, boolean passos, boolean batimentos) {
        return "telemetry:hoje:v1:" + IDOSO + ":" + dia + ":" + passos + ":" + batimentos;
    }
}
