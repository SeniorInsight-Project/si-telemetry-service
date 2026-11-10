package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.porta.saida.RegistroSincronizacaoAparelho;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioStatusAparelho;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioStatusAparelho.Registro;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@TesteIntegracao
class RepositorioStatusAparelhoIntegracaoTest {

    @Autowired RepositorioStatusAparelho repositorio;
    @Autowired RegistroSincronizacaoAparelho sincronizacao;
    @Autowired JdbcTemplate jdbc;

    private final UUID idoso = UUID.randomUUID();

    @Test
    void semRegistroVemVazio() {
        assertThat(repositorio.buscar(idoso)).isEmpty();
    }

    @Test
    void leSincronizacaoEBateriaGravadasNaIngestao() {
        Instant quando = Instant.parse("2026-11-10T14:30:00Z");
        sincronizacao.registrar(idoso, quando, 55);

        assertThat(repositorio.buscar(idoso)).contains(new Registro(quando, 55, null, null, null));
    }

    @Test
    void leVersaoModeloESistema() {
        jdbc.update("""
                INSERT INTO status_aparelho (idoso_id, nivel_bateria, versao_app, modelo_celular, sistema_operacional)
                VALUES (?, 80, '1.0.4', 'Samsung Galaxy A15', 'Android 14')
                """, idoso);

        assertThat(repositorio.buscar(idoso)).contains(
                new Registro(null, 80, "1.0.4", "Samsung Galaxy A15", "Android 14"));
    }
}
