package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.RepositorioMedicoes;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Grava em {@code medicao} e soma ao {@code agregado_diario} em um único comando por amostra: o resumo só é
 * tocado quando a inserção não foi duplicada, então reenviar a mesma leitura não infla passos nem médias.
 * O dia do resumo é o do fuso configurado ({@code telemetry.hoje.fuso}), o mesmo usado pela consulta de "hoje".
 */
@Repository
class RepositorioMedicoesJdbc implements RepositorioMedicoes {

    private static final String GRAVAR = """
            WITH nova AS (
                INSERT INTO medicao (idoso_id, tipo, valor, unidade, medido_em, origem)
                VALUES (:idosoId, :tipo, :valor, :unidade, :medidoEm, :origem)
                ON CONFLICT (idoso_id, tipo, medido_em, origem) DO NOTHING
                RETURNING valor
            )
            INSERT INTO agregado_diario AS a (idoso_id, dia, tipo, unidade, quantidade, minimo, maximo, media, soma)
            SELECT :idosoId, :dia, :tipo, :unidade, 1, valor, valor, valor, valor FROM nova
            ON CONFLICT (idoso_id, dia, tipo) DO UPDATE
                SET quantidade = a.quantidade + 1,
                    minimo = LEAST(a.minimo, EXCLUDED.minimo),
                    maximo = GREATEST(a.maximo, EXCLUDED.maximo),
                    soma = a.soma + EXCLUDED.soma,
                    media = (a.soma + EXCLUDED.soma) / (a.quantidade + 1),
                    atualizado_em = now()
            """;

    private final JdbcClient jdbc;
    private final ZoneId fuso;

    RepositorioMedicoesJdbc(JdbcClient jdbc, @Value("${telemetry.hoje.fuso}") String fuso) {
        this.jdbc = jdbc;
        this.fuso = ZoneId.of(fuso);
    }

    @Override
    public int gravarNovas(UUID idosoId, List<Leitura> leituras) {
        int novas = 0;
        for (Leitura leitura : leituras) {
            novas += jdbc.sql(GRAVAR)
                    .param("idosoId", idosoId)
                    .param("tipo", leitura.tipo())
                    .param("valor", leitura.valor())
                    .param("unidade", leitura.unidade())
                    .param("medidoEm", leitura.medidaEm().atOffset(ZoneOffset.UTC))
                    .param("origem", leitura.origem())
                    .param("dia", leitura.medidaEm().atZone(fuso).toLocalDate())
                    .update();
        }
        return novas;
    }
}
