package com.seniorinsight.telemetry.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** CloudEvent v1 para o contrato telemetry.signal.recovered.v1. */
public record TelemetrySignalRecoveredEvent(
        String specversion, String id, String source, String type,
        Instant time, String subject, Map<String, Object> data) {
    public static TelemetrySignalRecoveredEvent create(UUID idosoId, Instant synchronizedAt) {
        return new TelemetrySignalRecoveredEvent("1.0", UUID.randomUUID().toString(), "/si-telemetry-service",
                "telemetry.signal.recovered.v1", synchronizedAt, idosoId.toString(),
                Map.of("idoso_id", idosoId, "synchronized_at", synchronizedAt));
    }
}