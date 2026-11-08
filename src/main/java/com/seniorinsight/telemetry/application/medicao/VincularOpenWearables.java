package com.seniorinsight.telemetry.application.medicao;

import java.util.Optional;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables.Vinculo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VincularOpenWearables {

    private final StatusAparelhoRepositorio consentimentos;
    private final VinculosOpenWearables vinculos;
    private final OpenWearables openWearables;
    private final ReferenciaOpenWearables referencias;

    public VincularOpenWearables(StatusAparelhoRepositorio consentimentos, VinculosOpenWearables vinculos,
            OpenWearables openWearables, ReferenciaOpenWearables referencias) {
        this.consentimentos = consentimentos;
        this.vinculos = vinculos;
        this.openWearables = openWearables;
        this.referencias = referencias;
    }

    @Transactional
    public Optional<Vinculo> obter(UUID idosoId) {
        consentimentos.bloquearConsentimentos(idosoId);
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
        vinculos.vincular(idosoId, usuarioId);
        return Optional.of(new Vinculo(usuarioId, null));
    }
}
