package com.seniorinsight.telemetry.adapters.out.cache;

import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.telemetry.application.porta.saida.CacheMedicoesHoje;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class CacheMedicoesHojeRedis implements CacheMedicoesHoje {

    private static final Duration TTL = Duration.ofMinutes(15);

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
            return valor == null ? Optional.empty() : Optional.ofNullable(json.readValue(valor, MedicoesHoje.class));
        } catch (DataAccessException | JsonProcessingException indisponivel) {
            return Optional.empty();
        }
    }

    @Override
    public void guardar(String chave, MedicoesHoje medicoes) {
        if (ativo) {
            try {
                redis.opsForValue().set(chave, json.writeValueAsString(medicoes), TTL);
            } catch (DataAccessException | JsonProcessingException indisponivel) {
                // A leitura permanece disponível pelo PostgreSQL.
            }
        }
    }
}
