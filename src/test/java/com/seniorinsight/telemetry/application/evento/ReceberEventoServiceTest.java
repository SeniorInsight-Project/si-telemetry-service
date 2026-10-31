package com.seniorinsight.telemetry.application.evento;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import com.seniorinsight.telemetry.application.porta.saida.RegistroEventosRecebidos;
import org.junit.jupiter.api.Test;

class ReceberEventoServiceTest {

    private final RegistroEventosRecebidos registro = mock(RegistroEventosRecebidos.class);
    private final TratadorEvento tratadorConsentimento = mock(TratadorEvento.class);

    private ReceberEventoService servico() {
        when(tratadorConsentimento.tipoEvento()).thenReturn("consent.changed");
        return new ReceberEventoService(registro, List.of(tratadorConsentimento));
    }

    @Test
    void eventoNovoEhRegistradoETratado() {
        ReceberEventoService servico = servico();
        EventoRecebido evento = new EventoRecebido(UUID.randomUUID(), "consent.changed", "{}");
        when(registro.registrarSeNovo(evento.id(), "consent.changed")).thenReturn(true);

        servico.receber(evento);

        verify(tratadorConsentimento).tratar(evento);
    }

    @Test
    void eventoRepetidoNaoEhTratadoDeNovo() {
        ReceberEventoService servico = servico();
        EventoRecebido evento = new EventoRecebido(UUID.randomUUID(), "consent.changed", "{}");
        when(registro.registrarSeNovo(any(), anyString())).thenReturn(false);

        servico.receber(evento);

        verify(tratadorConsentimento, never()).tratar(any());
    }

    @Test
    void eventoSemTratadorEhSoRegistrado() {
        ReceberEventoService servico = servico();
        EventoRecebido evento = new EventoRecebido(UUID.randomUUID(), "outro.evento", "{}");
        when(registro.registrarSeNovo(any(), anyString())).thenReturn(true);

        servico.receber(evento);

        verify(registro).registrarSeNovo(evento.id(), "outro.evento");
        verify(tratadorConsentimento, never()).tratar(any());
    }
}
