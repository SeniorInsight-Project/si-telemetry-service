package com.seniorinsight.telemetry.domain.evento;

import java.time.Instant;
import java.util.UUID;

/**
 * O relógio do idoso confirmou uma queda. Publicado como {@code telemetry.fall.detected} para o serviço de
 * notificações decidir o alerta.
 *
 * @param idosoId     idoso que sofreu a queda
 * @param detectadaEm instante do impacto (ou da leitura, se o relógio não informou)
 * @param forcaImpactoG força do impacto em múltiplos da gravidade, se informada
 * @param confianca   confiança da detecção de 0 a 1, se informada
 */
public record QuedaDetectada(UUID idosoId, Instant detectadaEm, Double forcaImpactoG, Double confianca)
        implements EventoDominio {

    @Override
    public String tipoAgregado() {
        return "idoso";
    }

    @Override
    public UUID agregadoId() {
        return idosoId;
    }

    @Override
    public String tipoEvento() {
        return "telemetry.fall.detected";
    }
}
