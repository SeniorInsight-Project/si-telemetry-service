package com.seniorinsight.telemetry.adapters.out.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.porta.saida.CacheMedicoesHoje;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Redis real em container: guarda, lê e expira em 15 min. */
@TesteIntegracao
class CacheMedicoesHojeRedisTest {

    @Autowired CacheMedicoesHoje cache;
    @Autowired StringRedisTemplate redis;

    @Test
    void guardaELeDeVolta() {
        String chave = "telemetry:hoje:v1:" + UUID.randomUUID();
        MedicoesHoje medicoes = new MedicoesHoje(3200L, 78, null, Instant.parse("2026-11-10T14:30:00Z"));

        cache.guardar(chave, medicoes);

        assertThat(cache.buscar(chave)).contains(medicoes);
    }

    @Test
    void chaveInexistenteEhMiss() {
        assertThat(cache.buscar("telemetry:hoje:v1:" + UUID.randomUUID())).isEmpty();
    }

    @Test
    void entradaExpiraEm15Minutos() {
        String chave = "telemetry:hoje:v1:" + UUID.randomUUID();

        cache.guardar(chave, MedicoesHoje.VAZIO);

        Duration ttl = Duration.ofSeconds(redis.getExpire(chave));
        assertThat(ttl).isBetween(Duration.ofMinutes(14), Duration.ofMinutes(15));
    }
}
