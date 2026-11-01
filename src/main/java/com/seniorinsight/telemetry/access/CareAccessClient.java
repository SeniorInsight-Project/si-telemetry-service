package com.seniorinsight.telemetry.access;

import java.util.UUID;

public interface CareAccessClient {
    boolean canReadTelemetry(UUID idosoId, String userId, String role, String bearerToken);
}
