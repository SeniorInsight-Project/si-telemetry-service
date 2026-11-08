package com.seniorinsight.telemetry.application.evento;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import com.seniorinsight.telemetry.application.porta.entrada.ReceberEvento;
import com.seniorinsight.telemetry.application.porta.saida.RegistroEventosRecebidos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra o id do evento e chama o tratador do tipo na mesma transação: ou o evento fica registrado e tratado, ou
 * nada fica gravado. Tipos sem tratador são só registrados.
 */
@Service
class ReceberEventoService implements ReceberEvento {

    private static final Logger log = LoggerFactory.getLogger(ReceberEventoService.class);

    private final RegistroEventosRecebidos registro;
    private final Map<String, TratadorEvento> tratadores;

    ReceberEventoService(RegistroEventosRecebidos registro, List<TratadorEvento> tratadores) {
        this.registro = registro;
        this.tratadores = tratadores.stream()
                .collect(Collectors.toUnmodifiableMap(TratadorEvento::tipoEvento, Function.identity()));
    }

    @Override
    @Transactional
    public void receber(EventoRecebido evento) {
        if (!registro.registrarSeNovo(evento.id(), evento.tipo())) {
            log.debug("Evento {} ({}) já processado, ignorado", evento.id(), evento.tipo());
            return;
        }
        TratadorEvento tratador = tratadores.get(evento.tipo());
        if (tratador == null) {
            log.debug("Evento {} ({}) sem tratador", evento.id(), evento.tipo());
            return;
        }
        tratador.tratar(evento);
    }
}
