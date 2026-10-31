package com.seniorinsight.telemetry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.seniorinsight.telemetry.application.evento.TratadorEvento;
import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Tratadores de eventos {@code teste.*} para exercitar o consumidor da fila sem depender das regras de negócio. */
@TestConfiguration(proxyBeanMethods = false)
public class TratadoresTeste {

    public static final String TIPO_CONTADO = "teste.contado";
    public static final String TIPO_FALHA = "teste.falha";

    @Bean
    Contador contador() {
        return new Contador();
    }

    @Bean
    TratadorEvento tratadorContado(Contador contador) {
        return new TratadorEvento() {
            @Override
            public String tipoEvento() {
                return TIPO_CONTADO;
            }

            @Override
            public void tratar(EventoRecebido evento) {
                contador.incrementar(evento.id());
            }
        };
    }

    @Bean
    TratadorEvento tratadorFalha() {
        return new TratadorEvento() {
            @Override
            public String tipoEvento() {
                return TIPO_FALHA;
            }

            @Override
            public void tratar(EventoRecebido evento) {
                throw new IllegalStateException("falha simulada no tratador");
            }
        };
    }

    /** Quantas vezes cada evento {@code teste.contado} foi tratado. */
    public static class Contador {

        private final Map<UUID, AtomicInteger> tratamentos = new ConcurrentHashMap<>();

        void incrementar(UUID eventoId) {
            tratamentos.computeIfAbsent(eventoId, id -> new AtomicInteger()).incrementAndGet();
        }

        public int vezes(UUID eventoId) {
            AtomicInteger vezes = tratamentos.get(eventoId);
            return vezes == null ? 0 : vezes.get();
        }
    }
}
