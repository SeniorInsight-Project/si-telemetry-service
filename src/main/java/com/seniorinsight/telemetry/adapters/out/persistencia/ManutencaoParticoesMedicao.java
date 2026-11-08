package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cria com antecedência as partições mensais de {@code medicao} (mês em UTC), na subida e uma vez por dia. O que
 * cair fora delas vai para a partição padrão {@code medicao_padrao}.
 */
@Component
class ManutencaoParticoesMedicao {

    private static final Logger log = LoggerFactory.getLogger(ManutencaoParticoesMedicao.class);

    private final JdbcTemplate jdbc;
    private final int mesesAFrente;

    ManutencaoParticoesMedicao(JdbcTemplate jdbc, @Value("${telemetry.particoes.meses-a-frente}") int mesesAFrente) {
        this.jdbc = jdbc;
        this.mesesAFrente = mesesAFrente;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${telemetry.particoes.cron}", zone = "UTC")
    public void garantirParticoes() {
        LocalDate mesAtual = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        for (int i = 0; i <= mesesAFrente; i++) {
            LocalDate mes = mesAtual.plusMonths(i);
            try {
                Boolean criada = jdbc.queryForObject("SELECT criar_particao_medicao(?)", Boolean.class, mes);
                if (Boolean.TRUE.equals(criada)) {
                    log.info("Partição de medicao criada para {}", mes);
                }
            } catch (DataAccessException e) {
                // Outra réplica pode estar criando a mesma partição; a próxima execução confere de novo.
                log.warn("Não foi possível criar a partição de medicao para {}: {}", mes, e.getMessage());
            }
        }
    }
}
