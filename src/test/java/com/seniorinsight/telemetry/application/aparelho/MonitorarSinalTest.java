package com.seniorinsight.telemetry.application.aparelho;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.MonitoramentoSinalRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.domain.evento.SinalPerdido;
import org.junit.jupiter.api.Test;

class MonitorarSinalTest {

    private final MonitoramentoSinalRepositorio repositorio = mock(MonitoramentoSinalRepositorio.class);
    private final PublicadorEventos eventos = mock(PublicadorEventos.class);
    private final MonitorarSinal monitor = new MonitorarSinal(repositorio, eventos, Duration.ofHours(3),
            "America/Sao_Paulo", "07:00", "22:00");

    @Test
    void noInicioDaJanelaPublicaPerdasComTresHorasDeTolerancia() {
        Instant agora = Instant.parse("2026-11-02T10:00:00Z");
        Instant limite = Instant.parse("2026-11-02T07:00:00Z");
        var perda = new SinalPerdido(UUID.randomUUID(), limite, agora);
        when(repositorio.registrarPerdas(limite, agora)).thenReturn(List.of(perda));

        monitor.verificar(agora);

        verify(eventos).publicar(perda);
    }

    @Test
    void naoProcuraPerdasDuranteOSilencioNoturno() {
        monitor.verificar(Instant.parse("2026-11-02T01:00:00Z"));
        monitor.verificar(Instant.parse("2026-11-02T03:30:00Z"));

        verifyNoInteractions(repositorio, eventos);
    }
}
