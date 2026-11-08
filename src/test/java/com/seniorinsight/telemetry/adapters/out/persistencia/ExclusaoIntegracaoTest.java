package com.seniorinsight.telemetry.adapters.out.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.telemetry.TesteIntegracao;
import com.seniorinsight.telemetry.application.lgpd.TratarExclusao;
import com.seniorinsight.telemetry.application.medicao.ReferenciaOpenWearables;
import com.seniorinsight.telemetry.application.porta.entrada.EventoRecebido;
import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.ExclusaoDadosRepositorio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@TesteIntegracao
@Transactional
class ExclusaoIntegracaoTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entidades;
    @Autowired ObjectMapper json;
    @Autowired PublicadorEventos eventos;
    @Autowired VinculosOpenWearables vinculos;
    @Autowired ReferenciaOpenWearables referencias;
    @Autowired ExclusaoDadosRepositorio repositorio;

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
        var tratador = new TratarExclusao(json, repositorio, eventos, provedor, vinculos, referencias);

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
        var tratador = new TratarExclusao(json, repositorio, eventos, provedor, vinculos, referencias);

        assertThatThrownBy(() -> enviar(tratador, protocolo, titular)).hasMessageContaining("reconciliação do vínculo");
        verifyNoInteractions(provedor);
        assertThat(jdbc.queryForList("SELECT id FROM medicao WHERE idoso_id = ?", UUID.class, titular)).hasSize(1);
        assertThat(jdbc.queryForList("SELECT id FROM outbox_evento WHERE agregado_id = ?", UUID.class, protocolo))
                .isEmpty();
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
        UUID mensagem = UUID.randomUUID();
        String corpo = """
                {"specversion":"1.0","id":"%s","source":"/report-service","type":"lgpd.erasure.requested",
                 "time":"2026-11-08T15:30:00Z","datacontenttype":"application/json",
                 "dataschema":"https://raw.githubusercontent.com/SeniorInsight-Project/si-contracts/main/events/lgpd.erasure.requested.v1.json",
                 "subject":"%s","data":{"protocolo":"%s","titularId":"%s","tipoTitular":"%s","titularSub":"%s"}}
                """.formatted(mensagem, protocolo, protocolo, titular, "IDOSO", "sub-idoso");
        tratador.tratar(new EventoRecebido(mensagem, TratarExclusao.TIPO, corpo));
    }
}
