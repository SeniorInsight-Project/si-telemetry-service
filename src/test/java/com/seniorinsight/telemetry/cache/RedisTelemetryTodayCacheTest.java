package com.seniorinsight.telemetry.cache;

import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seniorinsight.telemetry.dto.TelemetryTodayResponseDTO;

class RedisTelemetryTodayCacheTest {
    @SuppressWarnings("unchecked")
    @Test
    void gravaComTtlConfiguradoDeQuinzeMinutos() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        RedisTelemetryTodayCache cache = new RedisTelemetryTodayCache(redis, objectMapper,
                Duration.ofMinutes(15), true);
        TelemetryTodayResponseDTO response = new TelemetryTodayResponseDTO(UUID.randomUUID(), 2_340, 72,
                Instant.parse("2026-11-01T14:30:00Z"), TelemetryTodayResponseDTO.UPDATED, null);

        cache.put("telemetry:today:key", response);

        verify(values).set(eq("telemetry:today:key"), anyString(), eq(Duration.ofMinutes(15)));
    }
}