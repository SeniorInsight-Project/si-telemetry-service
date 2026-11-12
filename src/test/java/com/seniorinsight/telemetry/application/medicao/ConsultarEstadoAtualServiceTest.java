package com.seniorinsight.telemetry.application.medicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.AcessoNegadoException;
import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.entrada.EstadoAtual;
import com.seniorinsight.telemetry.application.porta.saida.CacheMedicoesHoje;
import com.seniorinsight.telemetry.application.porta.saida.ConsentimentosRede;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaMedicoesHoje;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaVinculo;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.consentimento.TipoDado;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.junit.jupiter.api.Test;

/** Cenários da HU-030 (passos) e da HU-031 (batimentos): autorização, recência de 48 h e cache. */
class ConsultarEstadoAtualServiceTest {

    private static final UUID IDOSO = UUID.randomUUID();
    private static final String SUB = "sub-1";
    private static final Instant AGORA = Instant.parse("2026-11-10T15:00:00Z");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Instant HA_UMA_HORA = AGORA.minusSeconds(3600);

    private final ConsultaVinculo vinculo = mock(ConsultaVinculo.class);
    private final ConsentimentosRede consentimentos = mock(ConsentimentosRede.class);
    private final ConsultaMedicoesHoje medicoes = mock(ConsultaMedicoesHoje.class);
    private final CacheMedicoesHoje cache = mock(CacheMedicoesHoje.class);
    private final ConsultarEstadoAtualService servico = new ConsultarEstadoAtualService(
            new VerificarAcessoIdoso(vinculo), consentimentos, medicoes, cache,
            Clock.fixed(AGORA, ZoneOffset.UTC), FUSO.getId(), 4000);

    ConsultarEstadoAtualServiceTest() {
        when(vinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.of(Papel.FAMILIAR_APOIO));
        when(cache.buscar(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void devolvePassosDoDiaEUltimoBatimentoDosTiposAutorizados() {
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS, TipoDado.BATIMENTOS));
        when(medicoes.buscar(IDOSO, LocalDate.parse("2026-11-10"), AGORA, true, true))
                .thenReturn(new MedicoesHoje(3200L, 78, 64, HA_UMA_HORA));

        EstadoAtual estado = servico.consultar(IDOSO, SUB);

        assertThat(estado).isEqualTo(new EstadoAtual(IDOSO, 3200L, 4000, 78, 64, HA_UMA_HORA));
    }

    @Test
    void diaDaConsultaSegueOFusoConfigurado() {
        // 02:00Z de 11/11 ainda é 10/11 em São Paulo.
        Instant madrugada = Instant.parse("2026-11-11T02:00:00Z");
        var noite = new ConsultarEstadoAtualService(new VerificarAcessoIdoso(vinculo), consentimentos, medicoes,
                cache, Clock.fixed(madrugada, ZoneOffset.UTC), FUSO.getId(), 4000);
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS));
        when(medicoes.buscar(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(MedicoesHoje.VAZIO);

        noite.consultar(IDOSO, SUB);

        verify(medicoes).buscar(IDOSO, LocalDate.parse("2026-11-10"), madrugada, true, false);
    }

    @Test
    void batimentosRevogadosVoltamNuloMesmoComDadoNoBanco() {
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS));
        when(medicoes.buscar(IDOSO, LocalDate.parse("2026-11-10"), AGORA, true, false))
                .thenReturn(new MedicoesHoje(500L, null, null, HA_UMA_HORA));

        EstadoAtual estado = servico.consultar(IDOSO, SUB);

        assertThat(estado.passosHoje()).isEqualTo(500);
        assertThat(estado.frequenciaCardiaca()).isNull();
        assertThat(estado.frequenciaCardiacaRepouso()).isNull();
    }

    @Test
    void passosRevogadosVoltamNulo() {
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.BATIMENTOS));
        when(medicoes.buscar(IDOSO, LocalDate.parse("2026-11-10"), AGORA, false, true))
                .thenReturn(new MedicoesHoje(null, 80, 62, HA_UMA_HORA));

        EstadoAtual estado = servico.consultar(IDOSO, SUB);

        assertThat(estado.passosHoje()).isNull();
        assertThat(estado.frequenciaCardiaca()).isEqualTo(80);
        assertThat(estado.frequenciaCardiacaRepouso()).isEqualTo(62);
    }

    @Test
    void semNenhumTipoAutorizadoNaoLeNemOBancoNemOCache() {
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.SONO));

        EstadoAtual estado = servico.consultar(IDOSO, SUB);

        assertThat(estado).isEqualTo(new EstadoAtual(IDOSO, null, 4000, null, null, null));
        verifyNoInteractions(medicoes, cache);
    }

    @Test
    void depoisDe48HorasVemSemDadosRecentes() {
        Instant antiga = AGORA.minusSeconds(48 * 3600 + 1);
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS, TipoDado.BATIMENTOS));
        when(medicoes.buscar(IDOSO, LocalDate.parse("2026-11-10"), AGORA, true, true))
                .thenReturn(new MedicoesHoje(0L, 70, 60, antiga));

        EstadoAtual estado = servico.consultar(IDOSO, SUB);

        assertThat(estado.passosHoje()).isNull();
        assertThat(estado.frequenciaCardiaca()).isNull();
        assertThat(estado.frequenciaCardiacaRepouso()).isNull();
        assertThat(estado.coletadoEm()).isEqualTo(antiga);
        verify(cache, never()).guardar(anyString(), any());
    }

    @Test
    void exatamente48HorasAindaContaComoRecente() {
        Instant limite = AGORA.minusSeconds(48 * 3600);
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.BATIMENTOS));
        when(medicoes.buscar(IDOSO, LocalDate.parse("2026-11-10"), AGORA, false, true))
                .thenReturn(new MedicoesHoje(null, 70, null, limite));

        assertThat(servico.consultar(IDOSO, SUB).frequenciaCardiaca()).isEqualTo(70);
    }

    @Test
    void semNenhumaLeituraColetadoEmVemNulo() {
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS, TipoDado.BATIMENTOS));
        when(medicoes.buscar(IDOSO, LocalDate.parse("2026-11-10"), AGORA, true, true))
                .thenReturn(MedicoesHoje.VAZIO);

        assertThat(servico.consultar(IDOSO, SUB)).isEqualTo(new EstadoAtual(IDOSO, null, 4000, null, null, null));
        verify(cache, never()).guardar(anyString(), any());
    }

    @Test
    void acertoNoCachePulaOBanco() {
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS, TipoDado.BATIMENTOS));
        when(cache.buscar(anyString())).thenReturn(Optional.of(new MedicoesHoje(900L, 75, 60, HA_UMA_HORA)));

        EstadoAtual estado = servico.consultar(IDOSO, SUB);

        assertThat(estado.passosHoje()).isEqualTo(900);
        verifyNoInteractions(medicoes);
    }

    @Test
    void leituraRecenteVaiParaOCacheComAChaveDoConsentimento() {
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS, TipoDado.BATIMENTOS));
        MedicoesHoje lida = new MedicoesHoje(100L, 70, 60, HA_UMA_HORA);
        when(medicoes.buscar(IDOSO, LocalDate.parse("2026-11-10"), AGORA, true, true)).thenReturn(lida);

        servico.consultar(IDOSO, SUB);

        verify(cache).guardar(eq("telemetry:hoje:v1:" + IDOSO + ":2026-11-10:true:true"), eq(lida));
    }

    @Test
    void chaveDoCacheMudaQuandoOConsentimentoMuda() {
        when(medicoes.buscar(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new MedicoesHoje(100L, 70, 60, HA_UMA_HORA));

        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS, TipoDado.BATIMENTOS));
        servico.consultar(IDOSO, SUB);
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS));
        servico.consultar(IDOSO, SUB);

        verify(cache).buscar("telemetry:hoje:v1:" + IDOSO + ":2026-11-10:true:true");
        verify(cache).buscar("telemetry:hoje:v1:" + IDOSO + ":2026-11-10:true:false");
    }

    @Test
    void semVinculoNegaSemTocarNosDados() {
        when(vinculo.papelDoUsuario(IDOSO, "estranho")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.consultar(IDOSO, "estranho")).isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(consentimentos, medicoes, cache);
    }

    @Test
    void medicoNaoLeOEstadoDeHoje() {
        when(vinculo.papelDoUsuario(IDOSO, "dr")).thenReturn(Optional.of(Papel.MEDICO));

        assertThatThrownBy(() -> servico.consultar(IDOSO, "dr")).isInstanceOf(AcessoNegadoException.class);
    }
}
