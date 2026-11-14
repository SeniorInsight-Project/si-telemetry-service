package com.seniorinsight.telemetry.application.porta.saida;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.domain.medicao.Leitura;

/**
 * API do Open Wearables, de onde vêm os dados dos wearables. Os webhooks dele ainda não existem, então a leitura é
 * por consulta periódica.
 *
 * <p>Falha de rede, timeout, 5xx e circuito aberto chegam como {@link OpenWearablesIndisponivelException}.
 */
public interface OpenWearables {

    Optional<UUID> buscarUsuarioPorReferencia(String referencia);

    UUID provisionarUsuario(String referencia);

    void excluirUsuario(UUID usuarioOpenWearables);

    /** Amostras dos tipos autorizados, juntando todas as páginas. Sem tipos, não consulta o provedor. */
    List<Leitura> buscarLeituras(UUID usuarioOpenWearables, Instant inicio, Instant fim, Set<String> tipos);
}
