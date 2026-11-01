package com.seniorinsight.telemetry.cache;

import java.util.Optional;
import com.seniorinsight.telemetry.dto.TelemetryTodayResponseDTO;

public interface TelemetryTodayCache {
    Optional<TelemetryTodayResponseDTO> get(String key);
    void put(String key, TelemetryTodayResponseDTO response);
}