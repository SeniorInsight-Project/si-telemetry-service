package com.seniorinsight.telemetry.application.medicao;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ColetarTelemetriaDoCiclo {

    private static final Logger log = LoggerFactory.getLogger(ColetarTelemetriaDoCiclo.class);

    private final VinculosOpenWearables vinculos;
    private final VincularOpenWearables provisionar;
    private final ColetarMedicoesAutorizadas coletar;

    public ColetarTelemetriaDoCiclo(VinculosOpenWearables vinculos, VincularOpenWearables provisionar,
                      ColetarMedicoesAutorizadas coletar) {
        this.vinculos = vinculos;
        this.provisionar = provisionar;
        this.coletar = coletar;
    }

    public void executar(Instant fim) {
        for (UUID idosoId : vinculos.idososAutorizados()) {
            try {
                vinculos.executarExclusivo(idosoId, () -> coletarIdoso(idosoId, fim));
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
