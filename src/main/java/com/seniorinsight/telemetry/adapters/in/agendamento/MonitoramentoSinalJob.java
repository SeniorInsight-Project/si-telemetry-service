package com.seniorinsight.telemetry.adapters.in.agendamento;

import java.time.Instant;

import com.seniorinsight.telemetry.application.aparelho.MonitorarSinal;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class MonitoramentoSinalJob {

    private final MonitorarSinal monitor;

    MonitoramentoSinalJob(MonitorarSinal monitor) {
        this.monitor = monitor;
    }

    @Scheduled(cron = "${telemetry.sinal.cron}", zone = "${telemetry.sinal.fuso}")
    void verificar() {
        monitor.verificar(Instant.now());
    }
}
