package com.seniorinsight.telemetry.application.aparelho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.AcessoNegadoException;
import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.entrada.StatusAparelho;
import com.seniorinsight.telemetry.application.porta.saida.ConsentimentosRede;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaVinculo;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioStatusAparelho;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioStatusAparelho.Registro;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.consentimento.TipoDado;
import org.junit.jupiter.api.Test;

/** Cenários da HU-027: o que o familiar vê do celular do idoso. */
class ConsultarStatusAparelhoServiceTest {

    private static final UUID IDOSO = UUID.randomUUID();
    private static final Instant SINCRONIZOU = Instant.parse("2026-11-10T14:30:00Z");

    private final ConsultaVinculo vinculo = mock(ConsultaVinculo.class);
    private final RepositorioStatusAparelho repositorio = mock(RepositorioStatusAparelho.class);
    private final ConsentimentosRede consentimentos = mock(ConsentimentosRede.class);
    private final ConsultarStatusAparelhoService servico = new ConsultarStatusAparelhoService(
            new VerificarAcessoIdoso(vinculo), repositorio, consentimentos);

    @Test
    void familiarVeSincronizacaoBateriaVersaoEModelo() {
        when(vinculo.papelDoUsuario(IDOSO, "fam")).thenReturn(Optional.of(Papel.FAMILIAR_APOIO));
        when(repositorio.buscar(IDOSO)).thenReturn(Optional.of(
                new Registro(SINCRONIZOU, 42, "1.0.4", "Samsung Galaxy A15", "Android 14")));
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS, TipoDado.BATIMENTOS));

        StatusAparelho status = servico.consultar(IDOSO, "fam");

        assertThat(status).isEqualTo(new StatusAparelho(IDOSO, SINCRONIZOU, 42, "1.0.4", "Samsung Galaxy A15",
                "Android 14", java.util.List.of(TipoDado.BATIMENTOS, TipoDado.PASSOS)));
    }

    @Test
    void idosoQueAindaNaoSincronizouRecebeCamposNulosETiposAtivos() {
        when(vinculo.papelDoUsuario(IDOSO, "idoso")).thenReturn(Optional.of(Papel.IDOSO));
        when(repositorio.buscar(IDOSO)).thenReturn(Optional.empty());
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of(TipoDado.PASSOS));

        StatusAparelho status = servico.consultar(IDOSO, "idoso");

        assertThat(status).isEqualTo(new StatusAparelho(IDOSO, null, null, null, null, null,
                java.util.List.of(TipoDado.PASSOS)));
    }

    @Test
    void tipoRevogadoNaoApareceEmTiposAtivos() {
        when(vinculo.papelDoUsuario(IDOSO, "adm")).thenReturn(Optional.of(Papel.FAMILIAR_ADMINISTRADOR));
        when(repositorio.buscar(IDOSO)).thenReturn(Optional.empty());
        when(consentimentos.tiposAutorizados(IDOSO)).thenReturn(Set.of());

        assertThat(servico.consultar(IDOSO, "adm").tiposAtivos()).isEmpty();
    }

    @Test
    void semVinculoOuMedicoNegaSemLerOStatus() {
        when(vinculo.papelDoUsuario(IDOSO, "estranho")).thenReturn(Optional.empty());
        when(vinculo.papelDoUsuario(IDOSO, "dr")).thenReturn(Optional.of(Papel.MEDICO));

        assertThatThrownBy(() -> servico.consultar(IDOSO, "estranho")).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> servico.consultar(IDOSO, "dr")).isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(repositorio, consentimentos);
    }
}
