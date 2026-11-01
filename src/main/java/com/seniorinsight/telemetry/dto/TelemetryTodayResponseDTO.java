package com.seniorinsight.telemetry.dto;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelemetryTodayResponseDTO(
        @JsonProperty("idoso_id") UUID idosoId,
        @JsonProperty("steps_today") Integer stepsToday,
        @JsonProperty("heart_rate") Integer heartRate,
        @JsonProperty("last_updated_at") Instant lastUpdatedAt,
        String status,
        String mensagem
) {
    public static final String UPDATED = "ATUALIZADO";
    public static final String NO_RECENT_DATA = "SEM_DADOS_RECENTES";
}