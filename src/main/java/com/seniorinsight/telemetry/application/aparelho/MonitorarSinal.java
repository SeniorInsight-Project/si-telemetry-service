package com.seniorinsight.telemetry.application.aparelho;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import com.seniorinsight.telemetry.application.porta.saida.MonitoramentoSinalRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitorarSinal {

    private final MonitoramentoSinalRepositorio repositorio;
    private final PublicadorEventos eventos;
    private final Duration tolerancia;
    private final ZoneId fuso;
    private final LocalTime inicio;
    private final LocalTime fim;

    public MonitorarSinal(MonitoramentoSinalRepositorio repositorio, PublicadorEventos eventos,
            @Value("${telemetry.sinal.tolerancia}") Duration tolerancia,
            @Value("${telemetry.sinal.fuso}") String fuso,
            @Value("${telemetry.sinal.inicio}") String inicio,
            @Value("${telemetry.sinal.fim}") String fim) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.tolerancia = tolerancia;
        this.fuso = ZoneId.of(fuso);
        this.inicio = LocalTime.parse(inicio);
        this.fim = LocalTime.parse(fim);
        if (tolerancia.isNegative() || tolerancia.isZero() || !this.inicio.isBefore(this.fim)) {
            throw new IllegalArgumentException("Tolerância e janela de monitoramento de sinal inválidas");
        }
    }

    @Transactional
    public void verificar(Instant agora) {
        LocalTime hora = agora.atZone(fuso).toLocalTime();
        if (hora.isBefore(inicio) || !hora.isBefore(fim)) {
            return;
        }

        repositorio.registrarPerdas(agora.minus(tolerancia), agora).forEach(eventos::publicar);
    }
}
