package com.seniorinsight.telemetry.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceStatusResponseDTO(
        @JsonProperty("idoso_id") UUID idosoId,
        @JsonProperty("last_sync_at") Instant lastSyncAt,
        @JsonProperty("battery_percentage") Integer batteryPercentage,
        @JsonProperty("app_version") String appVersion,
        @JsonProperty("active_data_types") List<String> activeDataTypes
) {}
