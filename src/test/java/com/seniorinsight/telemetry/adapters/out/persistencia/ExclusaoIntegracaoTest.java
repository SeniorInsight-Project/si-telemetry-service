package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.lgpd.TratarExclusao;
import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.application.porta.saida.ExclusaoDadosRepositorio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

@TesteIntegracao
@Transactional
class ExclusaoIntegracaoTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entidades;
    @Autowired ObjectMapper json;
    @Autowired PublicadorEventos eventos;
    @Autowired ExclusaoDadosRepositorio repositorio;
    @Autowired TransactionOperations transacoes;

    @Test
    void apagaVinculoExternoEReuneContagensSemIdentificarOTitular() {
        UUID titular = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID protocolo = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_open_wearables (idoso_id, usuario_id) VALUES (?, ?)", titular, usuario);
        inserirMedicao(titular);
        jdbc.update("""
                INSERT INTO agregado_diario (idoso_id, dia, tipo, unidade, soma)
                VALUES (?, current_date, 'steps', 'count', 2340)
                """, titular);
        jdbc.update("INSERT INTO status_aparelho (idoso_id) VALUES (?)", titular);
        var provedor = mock(OpenWearables.class);
        var tratador = new TratarExclusao(repositorio, eventos, provedor, transacoes);

        enviar(tratador, protocolo, titular);
        enviar(tratador, protocolo, titular);

        verify(provedor).excluirUsuario(usuario);
        assertThat(jdbc.queryForList("SELECT id FROM medicao WHERE idoso_id = ?", UUID.class, titular)).isEmpty();
        assertThat(jdbc.queryForList("SELECT usuario_id FROM usuario_open_wearables WHERE idoso_id = ?",
                UUID.class, titular)).isEmpty();
        assertThat(jdbc.queryForList("SELECT categoria FROM telemetria_anonima ORDER BY conjunto", String.class))
                .containsExactly("steps", "steps");
        confirmar(protocolo);
        assertThatThrownBy(() -> inserirMedicao(titular)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void semVinculoExternoDadosLegadosBloqueiamAConfirmacao() {
        UUID titular = UUID.randomUUID();
        UUID protocolo = UUID.randomUUID();
        inserirMedicao(titular);
        var provedor = mock(OpenWearables.class);
        var tratador = new TratarExclusao(repositorio, eventos, provedor, transacoes);

        assertThatThrownBy(() -> enviar(tratador, protocolo, titular)).hasMessageContaining("reconciliação do vínculo");
        verifyNoInteractions(provedor);
        assertThat(jdbc.queryForList("SELECT id FROM medicao WHERE idoso_id = ?", UUID.class, titular)).hasSize(1);
        assertThat(jdbc.queryForList("SELECT id FROM outbox_evento WHERE agregado_id = ?", UUID.class, protocolo))
                .isEmpty();
    }

    @Test
    void falhaDoProvedorPreservaVinculoEPermiteTentarONovoPedido() {
        UUID titular = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID protocolo = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_open_wearables (idoso_id, usuario_id) VALUES (?, ?)", titular, usuario);
        var provedor = mock(OpenWearables.class);
        doThrow(new IllegalStateException("provedor indisponível")).doNothing().when(provedor).excluirUsuario(usuario);
        var tratador = new TratarExclusao(repositorio, eventos, provedor, transacoes);

        assertThatThrownBy(() -> enviar(tratador, protocolo, titular)).hasMessageContaining("indisponível");
        assertThat(repositorio.confirmado(protocolo)).isFalse();
        assertThat(jdbc.queryForList("SELECT usuario_id FROM usuario_open_wearables WHERE idoso_id = ?",
                UUID.class, titular)).containsExactly(usuario);
        enviar(tratador, protocolo, titular);
        confirmar(protocolo);
    }

    private void inserirMedicao(UUID titular) {
        jdbc.update("""
                INSERT INTO medicao (idoso_id, tipo, valor, unidade, medido_em, origem)
                VALUES (?, 'steps', 150, 'count', now(), 'teste')
                """, titular);
    }
    private void confirmar(UUID protocolo) {
        entidades.flush();
        assertThat(jdbc.queryForList("""
                SELECT payload->>'protocolo' FROM outbox_evento
                WHERE agregado_id = ? AND tipo_evento = 'lgpd.erasure.completed'
                """, String.class, protocolo)).containsExactly(protocolo.toString());
    }

    private void enviar(TratarExclusao tratador, UUID protocolo, UUID titular) {
        tratador.tratar(new TratarExclusao.Pedido(protocolo, titular, "sub-idoso", true));
    }
}
