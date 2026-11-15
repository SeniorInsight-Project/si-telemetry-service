package com.seniorinsight.telemetry.application.consentimento;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.entrada.RegistrarConsentimentoRede;
import com.seniorinsight.telemetry.application.porta.saida.ConsentimentosRede;
import com.seniorinsight.telemetry.domain.consentimento.TipoDado;
import org.springframework.stereotype.Service;

@Service
class RegistrarConsentimentoRedeService implements RegistrarConsentimentoRede {

    private final ConsentimentosRede consentimentos;

    RegistrarConsentimentoRedeService(ConsentimentosRede consentimentos) {
        this.consentimentos = consentimentos;
    }

    @Override
    public void registrar(UUID idosoId, TipoDado tipo, boolean autorizado, Instant ocorridoEm) {
        consentimentos.atualizar(idosoId, tipo, autorizado, ocorridoEm);
    }
}
