package com.seniorinsight.telemetry.application.acesso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.ConsultaVinculo;
import com.seniorinsight.telemetry.application.porta.saida.VinculoIndisponivelException;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import org.junit.jupiter.api.Test;

class VerificarAcessoIdosoTest {

    private static final UUID IDOSO = UUID.randomUUID();
    private static final String SUB = "sub-123";
    private static final Set<Papel> FAMILIARES = Set.of(Papel.FAMILIAR_ADMINISTRADOR, Papel.FAMILIAR_APOIO);

    private final ConsultaVinculo consultaVinculo = mock(ConsultaVinculo.class);
    private final VerificarAcessoIdoso verificar = new VerificarAcessoIdoso(consultaVinculo);

    @Test
    void familiarDeApoioVinculadoTemAcesso() {
        when(consultaVinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.of(Papel.FAMILIAR_APOIO));

        assertThat(verificar.exigir(IDOSO, SUB, FAMILIARES)).isEqualTo(Papel.FAMILIAR_APOIO);
    }

    @Test
    void semVinculoNaoTemAcesso() {
        when(consultaVinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificar.exigir(IDOSO, SUB, FAMILIARES))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void papelForaDosPermitidosNaoTemAcesso() {
        when(consultaVinculo.papelDoUsuario(IDOSO, SUB)).thenReturn(Optional.of(Papel.MEDICO));

        assertThatThrownBy(() -> verificar.exigir(IDOSO, SUB, FAMILIARES))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void careServiceForaDoArPropagaIndisponibilidade() {
        when(consultaVinculo.papelDoUsuario(IDOSO, SUB))
                .thenThrow(new VinculoIndisponivelException("timeout", null));

        assertThatThrownBy(() -> verificar.exigir(IDOSO, SUB, FAMILIARES))
                .isInstanceOf(VinculoIndisponivelException.class);
    }
}
