package com.seniorinsight.telemetry.application.medicao;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.RegistroMedicoes;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class ColetarMedicoesAutorizadas {

    private final StatusAparelhoRepositorio consentimentos;
    private final OpenWearables openWearables;
    private final RegistroMedicoes registro;
    private final TransactionOperations transacoes;

    public ColetarMedicoesAutorizadas(StatusAparelhoRepositorio consentimentos, OpenWearables openWearables,
            RegistroMedicoes registro, TransactionOperations transacoes) {
        this.consentimentos = consentimentos;
        this.openWearables = openWearables;
        this.registro = registro;
        this.transacoes = transacoes;
    }

    public void coletar(UUID idosoId, UUID usuarioOpenWearables, Instant inicio, Instant fim) {
        Set<String> solicitados = tiposDeSerie(consentimentos.buscarTiposAtivosRede(idosoId));
        if (solicitados.isEmpty()) {
            return;
        }

        List<Leitura> leituras = openWearables.buscarLeituras(usuarioOpenWearables, inicio, fim, solicitados);
        if (leituras.isEmpty()) {
            return;
        }

        transacoes.executeWithoutResult(transacao -> {
            // A projeção da revogação usa o mesmo bloqueio. Nenhum lock fica aberto durante a chamada HTTP.
            consentimentos.bloquearConsentimentos(idosoId);
            Set<String> aindaAutorizados = tiposDeSerie(consentimentos.buscarTiposAtivosRede(idosoId));
            var filtradas = leituras.stream()
                    .filter(leitura -> solicitados.contains(leitura.tipo()) && aindaAutorizados.contains(leitura.tipo()))
                    .filter(leitura -> !leitura.medidaEm().isBefore(inicio) && !leitura.medidaEm().isAfter(fim))
                    .filter(ColetarMedicoesAutorizadas::valorValido)
                    .toList();
            if (!filtradas.isEmpty()) {
                registro.registrar(idosoId, filtradas);
            }
        });
    }

    private static Set<String> tiposDeSerie(Collection<TipoDado> autorizados) {
        return autorizados.stream().flatMap(tipo -> switch (tipo) {
            case BATIMENTOS -> List.of("heart_rate", "resting_heart_rate").stream();
            case PASSOS -> List.of("steps").stream();
            case OXIGENACAO -> List.of("oxygen_saturation").stream();
            case SONO, LOCALIZACAO -> List.<String>of().stream();
        }).collect(Collectors.toUnmodifiableSet());
    }

    private static boolean valorValido(Leitura leitura) {
        double valor = leitura.valor();
        if (!Double.isFinite(valor)) {
            return false;
        }
        return switch (leitura.tipo()) {
            case "heart_rate", "resting_heart_rate" -> valor >= 25 && valor <= 250;
            case "oxygen_saturation" -> valor >= 50 && valor <= 100;
            case "steps" -> valor >= 0;
            default -> false;
        };
    }
}
