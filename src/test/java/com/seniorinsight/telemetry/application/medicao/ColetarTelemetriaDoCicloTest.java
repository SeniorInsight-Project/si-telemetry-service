package com.seniorinsight.telemetry.application.medicao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import org.junit.jupiter.api.Test;

class ColetarTelemetriaDoCicloTest {
    private final VinculosOpenWearables vinculos = mock(VinculosOpenWearables.class);
    private final VincularOpenWearables provisionar = mock(VincularOpenWearables.class);
    private final ColetarMedicoesAutorizadas coletar = mock(ColetarMedicoesAutorizadas.class);
    private final ColetarTelemetriaDoCiclo ciclo = new ColetarTelemetriaDoCiclo(vinculos, provisionar, coletar);
    private final UUID idoso = UUID.randomUUID();
    private final Instant fim = Instant.parse("2026-11-12T12:00:00Z");

    @Test
    void limitaJanelaAoInicioDaAutorizacaoEAvancaCursor() {
        UUID usuario = UUID.randomUUID();
        Instant inicio = fim.minusSeconds(900);
        when(vinculos.idososAutorizados()).thenReturn(List.of(idoso));
        when(vinculos.executarExclusivo(eq(idoso), any())).thenAnswer(invocacao -> {
            invocacao.<Runnable>getArgument(1).run();
            return true;
        });
        when(provisionar.obter(idoso)).thenReturn(Optional.of(new VinculosOpenWearables.Vinculo(usuario, null)));
        when(vinculos.inicioAutorizado(idoso)).thenReturn(Optional.of(inicio));

        ciclo.executar(fim);
        verify(coletar).coletar(idoso, usuario, inicio, fim);
        verify(vinculos).avancar(idoso, fim);
    }

    @Test
    void replicaSemBloqueioNaoChamaProvedor() {
        when(vinculos.idososAutorizados()).thenReturn(List.of(idoso));
        ciclo.executar(fim);
        verifyNoInteractions(provisionar, coletar);
    }
}
