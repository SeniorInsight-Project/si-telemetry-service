package com.seniorinsight.telemetry.application.porta.entrada;

import java.util.UUID;

/** Status do celular do idoso (HU-027): última sincronização, bateria, versão do app e tipos ativos. */
public interface ConsultarStatusAparelho {

    /**
     * @param sub {@code sub} do token de quem consulta
     * @throws com.seniorinsight.telemetry.application.acesso.AcessoNegadoException se não houver vínculo com acesso
     */
    StatusAparelho consultar(UUID idosoId, String sub);
}
