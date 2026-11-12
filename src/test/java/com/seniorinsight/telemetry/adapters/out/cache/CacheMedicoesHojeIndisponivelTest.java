package com.seniorinsight.telemetry.adapters.out.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

/** O cache é opcional: desligado ou com o Redis fora do ar, nunca derruba a consulta. */
class CacheMedicoesHojeIndisponivelTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    @Test
    void redisForaDoArVirouMissESemExcecao() {
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("fora"));
        var cache = new CacheMedicoesHojeRedis(redis, new ObjectMapper(), true);

        assertThat(cache.buscar("k")).isEmpty();
        assertThatCode(() -> cache.guardar("k", MedicoesHoje.VAZIO)).doesNotThrowAnyException();
    }

    @Test
    void desligadoNemTocaNoRedis() {
        var cache = new CacheMedicoesHojeRedis(redis, new ObjectMapper(), false);

        assertThat(cache.buscar("k")).isEmpty();
        cache.guardar("k", MedicoesHoje.VAZIO);

        verifyNoInteractions(redis);
    }
}
