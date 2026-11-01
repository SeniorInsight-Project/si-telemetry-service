package com.seniorinsight.telemetry.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** CloudEvent v1 para o contrato telemetry.signal.lost.v1. */
public record TelemetrySignalLostEvent(
        String specversion, String id, String source, String type,
        Instant time, String subject, Map<String, Object> data) {
    public static TelemetrySignalLostEvent create(UUID idosoId, Instant lastSyncAt, Instant detectedAt) {
        return new TelemetrySignalLostEvent("1.0", UUID.randomUUID().toString(), "/si-telemetry-service",
                "telemetry.signal.lost.v1", detectedAt, idosoId.toString(),
                Map.of("idoso_id", idosoId, "last_sync_at", lastSyncAt, "detected_at", detectedAt));
    }
}