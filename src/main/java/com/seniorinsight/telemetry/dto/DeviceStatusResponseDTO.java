package com.seniorinsight.telemetry.dto;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceStatusResponseDTO(
        @JsonProperty("idosoId") UUID idosoId,
        @JsonProperty("ultimaSincronizacao") Instant lastSyncAt,
        @JsonProperty("nivelBateria") Integer batteryPercentage,
        @JsonProperty("versaoApp") String appVersion,
        @JsonProperty("tiposAtivos") List<String> activeDataTypes
) {
    public DeviceStatusResponseDTO {
        activeDataTypes = activeDataTypes == null ? List.of() : activeDataTypes.stream()
                .filter(Objects::nonNull)
                .map(DeviceStatusResponseDTO::contractType)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static String contractType(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "heart_rate", "batimentos" -> "BATIMENTOS";
            case "steps", "passos" -> "PASSOS";
            case "sleep", "sono" -> "SONO";
            case "spo2", "oxygen_saturation", "oxigenacao" -> "OXIGENACAO";
            case "location", "localizacao" -> "LOCALIZACAO";
            default -> null;
        };
    }
}
