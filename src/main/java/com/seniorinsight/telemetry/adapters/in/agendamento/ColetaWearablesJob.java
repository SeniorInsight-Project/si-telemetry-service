package com.seniorinsight.telemetry.adapters.in.agendamento;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.seniorinsight.telemetry.application.medicao.ColetarMedicoesAutorizadas;
import com.seniorinsight.telemetry.application.medicao.VincularOpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "telemetry.open-wearables.enabled", havingValue = "true")
class ColetaWearablesJob {

    private static final Logger log = LoggerFactory.getLogger(ColetaWearablesJob.class);

    private final VinculosOpenWearables vinculos;
    private final VincularOpenWearables provisionar;
    private final ColetarMedicoesAutorizadas coletar;

    ColetaWearablesJob(VinculosOpenWearables vinculos, VincularOpenWearables provisionar,
                      ColetarMedicoesAutorizadas coletar) {
        this.vinculos = vinculos;
        this.provisionar = provisionar;
        this.coletar = coletar;
    }

    @Scheduled(cron = "${telemetry.coleta.cron}")
    void executar() {
        Instant fim = Instant.now();
        for (UUID idosoId : vinculos.idososAutorizados()) {
            try {
                coletarIdoso(idosoId, fim);
            } catch (RuntimeException falha) {
                log.warn("Coleta individual não concluída; próxima execução tentará novamente ({})",
                        falha.getClass().getSimpleName());
            }
        }
    }

    private void coletarIdoso(UUID idosoId, Instant fim) {
        var vinculo = provisionar.obter(idosoId);
        var autorizacao = vinculos.inicioAutorizado(idosoId);
        if (vinculo.isEmpty() || autorizacao.isEmpty()) {
            return;
        }
        var conta = vinculo.get();
        // Sobreposição para sincronizações que chegam atrasadas; o banco deduplica.
        Instant inicio = conta.coletadoAte() == null ? fim.minus(24, ChronoUnit.HOURS)
                : conta.coletadoAte().minus(24, ChronoUnit.HOURS);
        if (inicio.isBefore(autorizacao.get())) {
            inicio = autorizacao.get();
        }
        if (inicio.isBefore(fim)) {
            coletar.coletar(idosoId, conta.usuarioId(), inicio, fim);
            vinculos.avancar(idosoId, fim);
        }
    }
}
