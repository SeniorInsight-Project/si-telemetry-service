package com.seniorinsight.telemetry.application.medicao;

import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.ConsentimentosRede;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables.Vinculo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class VincularOpenWearables {

    private final ConsentimentosRede consentimentos;
    private final VinculosOpenWearables vinculos;
    private final OpenWearables openWearables;
    private final ReferenciaOpenWearables referencias;
    private final TransactionOperations transacoes;

    public VincularOpenWearables(ConsentimentosRede consentimentos, VinculosOpenWearables vinculos,
            OpenWearables openWearables, ReferenciaOpenWearables referencias, TransactionOperations transacoes) {
        this.consentimentos = consentimentos;
        this.vinculos = vinculos;
        this.openWearables = openWearables;
        this.referencias = referencias;
        this.transacoes = transacoes;
    }

    public Optional<Vinculo> obter(UUID idosoId) {
        if (vinculos.inicioAutorizado(idosoId).isEmpty()) {
            return Optional.empty();
        }
        var existente = vinculos.buscar(idosoId);
        if (existente.isPresent()) {
            return existente;
        }
        String referencia = referencias.gerar(idosoId)
                .orElseThrow(() -> new IllegalStateException("Segredo de vínculo não configurado"));
        UUID usuarioId = openWearables.buscarUsuarioPorReferencia(referencia)
                .orElseGet(() -> {
                    if (vinculos.temDadosLegados(idosoId)) {
                        throw new IllegalStateException("Vínculo legado precisa de reconciliação antes de provisionar");
                    }
                    return openWearables.provisionarUsuario(referencia);
                });
        return transacoes.execute(status -> {
            consentimentos.bloquear(idosoId);
            // Mesmo após revogação, mantenha o vínculo para uma futura exclusão da conta.
            var atual = vinculos.buscar(idosoId);
            if (atual.isEmpty()) {
                vinculos.vincular(idosoId, usuarioId);
                atual = Optional.of(new Vinculo(usuarioId, null));
            }
            return vinculos.inicioAutorizado(idosoId).isPresent() ? atual : Optional.empty();
        });
    }
}
