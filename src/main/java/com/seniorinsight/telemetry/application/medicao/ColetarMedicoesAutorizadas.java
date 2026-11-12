package com.seniorinsight.telemetry.application.medicao;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.RegistroMedicoes;
import com.seniorinsight.telemetry.application.porta.saida.ConsentimentosRede;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import com.seniorinsight.telemetry.domain.medicao.TipoMedicao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class ColetarMedicoesAutorizadas {

    private final ConsentimentosRede consentimentos;
    private final OpenWearables openWearables;
    private final RegistroMedicoes registro;
    private final TransactionOperations transacoes;
    private final VinculosOpenWearables vinculos;

    public ColetarMedicoesAutorizadas(ConsentimentosRede consentimentos, OpenWearables openWearables,
            RegistroMedicoes registro, TransactionOperations transacoes, VinculosOpenWearables vinculos) {
        this.consentimentos = consentimentos;
        this.openWearables = openWearables;
        this.registro = registro;
        this.transacoes = transacoes;
        this.vinculos = vinculos;
    }

    public void coletar(UUID idosoId, UUID usuarioOpenWearables, Instant inicio, Instant fim) {
        Set<String> solicitados = TipoMedicao.seriesWearables(consentimentos.tiposAutorizados(idosoId));
        if (solicitados.isEmpty()) {
            return;
        }

        var inicioAutorizado = vinculos.inicioAutorizado(idosoId);
        if (inicioAutorizado.isEmpty()) {
            return;
        }
        Instant inicioConsulta = inicio.isBefore(inicioAutorizado.get()) ? inicioAutorizado.get() : inicio;
        if (!inicioConsulta.isBefore(fim)) {
            return;
        }
        List<Leitura> leituras = openWearables.buscarLeituras(usuarioOpenWearables, inicioConsulta, fim, solicitados);
        if (leituras.isEmpty()) {
            return;
        }

        transacoes.executeWithoutResult(transacao -> {
            // A projeção da revogação usa o mesmo bloqueio. Nenhum lock fica aberto durante a chamada HTTP.
            consentimentos.bloquear(idosoId);
            var inicioVigente = vinculos.inicioAutorizado(idosoId);
            if (inicioVigente.isEmpty()) {
                return;
            }
            Set<String> aindaAutorizados = TipoMedicao.seriesWearables(consentimentos.tiposAutorizados(idosoId));
            var filtradas = leituras.stream()
                    .filter(leitura -> solicitados.contains(leitura.tipo()) && aindaAutorizados.contains(leitura.tipo()))
                    .filter(leitura -> !leitura.medidaEm().isBefore(inicioConsulta) && !leitura.medidaEm().isAfter(fim))
                    .filter(leitura -> !leitura.medidaEm().isBefore(inicioVigente.get()))
                    .filter(TipoMedicao::leituraValida)
                    .toList();
            if (!filtradas.isEmpty()) {
                registro.registrar(idosoId, filtradas);
            }
        });
    }

}
