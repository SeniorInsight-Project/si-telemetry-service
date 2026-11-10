package com.seniorinsight.telemetry.application.porta.entrada;

import java.util.UUID;

/** Estado de hoje do idoso (HU-030, HU-031): passos do dia e último batimento, só dos tipos autorizados. */
public interface ConsultarEstadoAtual {

    /**
     * @param sub {@code sub} do token de quem consulta
     * @throws com.seniorinsight.telemetry.application.acesso.AcessoNegadoException se não houver vínculo com acesso
     */
    EstadoAtual consultar(UUID idosoId, String sub);
}
