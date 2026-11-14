package com.seniorinsight.telemetry.adapters.in.agendamento;

import java.time.Instant;

import com.seniorinsight.telemetry.application.medicao.ColetarTelemetriaDoCiclo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "telemetry.open-wearables.enabled", havingValue = "true")
class ColetaWearablesJob {
    private final ColetarTelemetriaDoCiclo ciclo;

    ColetaWearablesJob(ColetarTelemetriaDoCiclo ciclo) {
        this.ciclo = ciclo;
    }

    @Scheduled(cron = "${telemetry.coleta.cron}")
    void executar() {
        ciclo.executar(Instant.now());
    }
}
