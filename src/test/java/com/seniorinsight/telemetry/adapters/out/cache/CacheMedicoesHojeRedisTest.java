package com.seniorinsight.telemetry.adapters.out.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CacheMedicoesHojeRedisTest {

    @Test
    @SuppressWarnings("unchecked")
    void armazenaPorQuinzeMinutosEPreservaOsCamposNulos() {
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valores = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valores);
        var cache = new CacheMedicoesHojeRedis(redis, JsonMapper.builder().addModule(new JavaTimeModule()).build(), true);
        var medicoes = new MedicoesHoje(2340L, null, null, Instant.parse("2026-11-01T17:30:00Z"));

        cache.guardar("chave", medicoes);

        var json = ArgumentCaptor.forClass(String.class);
        verify(valores).set(eq("chave"), json.capture(), eq(Duration.ofMinutes(15)));
        when(valores.get("chave")).thenReturn(json.getValue());
        assertThat(cache.buscar("chave")).contains(medicoes);
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisIndisponivelRetornaCacheMiss() {
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valores = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valores);
        when(valores.get("chave")).thenThrow(new RedisConnectionFailureException("indisponível"));
        var cache = new CacheMedicoesHojeRedis(redis, JsonMapper.builder().build(), true);

        assertThat(cache.buscar("chave")).isEmpty();
    }
}
