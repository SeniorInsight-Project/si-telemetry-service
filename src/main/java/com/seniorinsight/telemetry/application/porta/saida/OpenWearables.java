package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.medicao.Leitura;

/**
 * API do Open Wearables, de onde vêm os dados dos wearables. Os webhooks dele ainda não existem, então a leitura é
 * por consulta periódica.
 *
 * <p>Falha de rede, timeout, 5xx e circuito aberto chegam como {@link OpenWearablesIndisponivelException}.
 */
public interface OpenWearables {

    /** Cria o usuário do idoso no Open Wearables e devolve o id gerado lá, que deve ser guardado. */
    UUID provisionarUsuario();

    /** Amostras do usuário com horário entre {@code inicio} e {@code fim}, juntando todas as páginas. */
    List<Leitura> buscarLeituras(UUID usuarioOpenWearables, Instant inicio, Instant fim);
}
