package com.seniorinsight.telemetry.event;
import java.time.Instant;
import java.util.UUID;
public record TelemetryFallDetectedEvent(UUID telemetryId, UUID idosoId, Instant occurredAt, Integer batteryLevel) {}
