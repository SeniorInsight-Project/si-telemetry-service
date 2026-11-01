package com.seniorinsight.telemetry.cache;

import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.telemetry.dto.TelemetryTodayResponseDTO;

@Component
public class RedisTelemetryTodayCache implements TelemetryTodayCache {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final boolean enabled;

    public RedisTelemetryTodayCache(StringRedisTemplate redis, ObjectMapper objectMapper,
            @Value("${seniorinsight.telemetry-today.cache-ttl:15m}") Duration ttl,
            @Value("${seniorinsight.telemetry-today.cache-enabled:true}") boolean enabled) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
        this.enabled = enabled;
    }

    @Override
    public Optional<TelemetryTodayResponseDTO> get(String key) {
        if (!enabled) return Optional.empty();
        try {
            String value = redis.opsForValue().get(key);
            return value == null ? Optional.empty()
                    : Optional.of(objectMapper.readValue(value, TelemetryTodayResponseDTO.class));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, TelemetryTodayResponseDTO response) {
        if (!enabled) return;
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(response), ttl);
        } catch (Exception ignored) {
            // Redis is an optimization; the database remains the source of truth.
        }
    }
}