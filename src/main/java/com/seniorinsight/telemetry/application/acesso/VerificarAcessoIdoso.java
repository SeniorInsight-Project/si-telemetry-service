package com.seniorinsight.telemetry.application.acesso;

import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.ConsultaVinculo;
import com.seniorinsight.telemetry.application.porta.saida.VinculoIndisponivelException;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import org.springframework.stereotype.Service;

/**
 * Verificação de papel e vínculo (RF-007): toda rota com {@code idosoId} chama {@link #exigir} antes de ler ou
 * gravar dados do idoso. O vínculo é do care-service.
 */
@Service
public class VerificarAcessoIdoso {

    private final ConsultaVinculo consultaVinculo;

    public VerificarAcessoIdoso(ConsultaVinculo consultaVinculo) {
        this.consultaVinculo = consultaVinculo;
    }

    /**
     * Devolve o papel do usuário no idoso se ele estiver entre os permitidos.
     *
     * @throws AcessoNegadoException        se não houver vínculo ou o papel não for permitido
     * @throws VinculoIndisponivelException se o care-service não respondeu
     */
    public Papel exigir(UUID idosoId, String sub, Set<Papel> papeisPermitidos) {
        return consultaVinculo.papelDoUsuario(idosoId, sub)
                .filter(papeisPermitidos::contains)
                .orElseThrow(() -> new AcessoNegadoException(idosoId));
    }
}
