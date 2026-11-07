package com.seniorinsight.telemetry.dto;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelemetryTodayResponseDTO(
        @JsonProperty("idosoId") UUID idosoId,
        @JsonInclude(JsonInclude.Include.ALWAYS) @JsonProperty("passosHoje") Integer stepsToday,
        @JsonInclude(JsonInclude.Include.ALWAYS) @JsonProperty("frequenciaCardiaca") Integer heartRate,
        @JsonInclude(JsonInclude.Include.ALWAYS) @JsonProperty("coletadoEm") Instant lastUpdatedAt,
        String status,
        String mensagem
) {
    public static final String UPDATED = "ATUALIZADO";
    public static final String NO_RECENT_DATA = "SEM_DADOS_RECENTES";
}
