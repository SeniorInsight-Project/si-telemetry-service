package com.seniorinsight.telemetry.adapters.out.cache;

import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.telemetry.application.porta.saida.CacheMedicoesHoje;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Cache no Redis com TTL de 15 min (RNF-002). É opcional: desligado ({@code telemetry.hoje.cache-redis-ativo=false})
 * ou com o Redis fora do ar, a consulta segue pelo PostgreSQL.
 */
@Component
class CacheMedicoesHojeRedis implements CacheMedicoesHoje {

    static final Duration TTL = Duration.ofMinutes(15);
    private static final Logger log = LoggerFactory.getLogger(CacheMedicoesHojeRedis.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final boolean ativo;

    CacheMedicoesHojeRedis(StringRedisTemplate redis, ObjectMapper json,
                           @Value("${telemetry.hoje.cache-redis-ativo}") boolean ativo) {
        this.redis = redis;
        this.json = json;
        this.ativo = ativo;
    }

    @Override
    public Optional<MedicoesHoje> buscar(String chave) {
        if (!ativo) {
            return Optional.empty();
        }
        try {
            String valor = redis.opsForValue().get(chave);
            return valor == null ? Optional.empty() : Optional.of(json.readValue(valor, MedicoesHoje.class));
        } catch (DataAccessException | JsonProcessingException e) {
            log.warn("Cache de hoje indisponível na leitura: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void guardar(String chave, MedicoesHoje medicoes) {
        if (!ativo) {
            return;
        }
        try {
            redis.opsForValue().set(chave, json.writeValueAsString(medicoes), TTL);
        } catch (DataAccessException | JsonProcessingException e) {
            log.warn("Cache de hoje indisponível na gravação: {}", e.getMessage());
        }
    }
}
