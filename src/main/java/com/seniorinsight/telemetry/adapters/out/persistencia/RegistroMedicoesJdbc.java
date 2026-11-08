package com.seniorinsight.telemetry.adapters.out.persistencia;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.application.porta.saida.RegistroMedicoes;
import com.seniorinsight.telemetry.domain.evento.TelemetriaIngerida;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
class RegistroMedicoesJdbc implements RegistroMedicoes {

    private final JdbcTemplate jdbc;
    private final PublicadorEventos eventos;
    private final ZoneId fuso;

    RegistroMedicoesJdbc(JdbcTemplate jdbc, PublicadorEventos eventos,
                         @Value("${telemetry.coleta.fuso}") String fuso) {
        this.jdbc = jdbc;
        this.eventos = eventos;
        this.fuso = ZoneId.of(fuso);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(UUID idosoId, List<Leitura> leituras) {
        var alterados = new LinkedHashSet<ChaveAgregado>();
        for (Leitura leitura : leituras) {
            LocalDate dia = leitura.medidaEm().atZone(fuso).toLocalDate();
            int atualizado = jdbc.update("""
                    WITH inserida AS (
                        INSERT INTO medicao (idoso_id, tipo, valor, unidade, medido_em, origem)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (idoso_id, tipo, medido_em, origem) DO NOTHING
                        RETURNING idoso_id, tipo, unidade, valor
                    )
                    INSERT INTO agregado_diario (idoso_id, dia, tipo, unidade, quantidade, minimo, maximo, media, soma)
                    SELECT idoso_id, ?, tipo, unidade, 1, valor, valor, valor, valor FROM inserida
                    ON CONFLICT (idoso_id, dia, tipo) DO UPDATE
                       SET quantidade = agregado_diario.quantidade + 1,
                           minimo = LEAST(agregado_diario.minimo, EXCLUDED.minimo),
                           maximo = GREATEST(agregado_diario.maximo, EXCLUDED.maximo),
                           soma = COALESCE(agregado_diario.soma, 0) + EXCLUDED.soma,
                           media = (COALESCE(agregado_diario.soma, 0) + EXCLUDED.soma)
                                   / (agregado_diario.quantidade + 1),
                           atualizado_em = now()
                    """, idosoId, leitura.tipo(), leitura.valor(), leitura.unidade(),
                    leitura.medidaEm().atOffset(ZoneOffset.UTC), leitura.origem(), dia);
            if (atualizado > 0) {
                alterados.add(new ChaveAgregado(dia, leitura.tipo()));
            }
        }
        if (!alterados.isEmpty()) {
            var agregados = alterados.stream().map(chave -> buscarAgregado(idosoId, chave)).toList();
            eventos.publicar(new TelemetriaIngerida(idosoId, agregados));
        }
    }

    private TelemetriaIngerida.Agregado buscarAgregado(UUID idosoId, ChaveAgregado chave) {
        var inicio = chave.dia().atStartOfDay(fuso).toOffsetDateTime();
        var fim = chave.dia().plusDays(1).atStartOfDay(fuso).toOffsetDateTime();
        return jdbc.queryForObject("""
                SELECT a.unidade, a.quantidade, a.minimo, a.maximo, a.media, a.soma,
                       (SELECT AVG(CASE WHEN h.tipo IN ('steps', 'sleep') THEN h.soma ELSE h.media END)
                          FROM agregado_diario h
                         WHERE h.idoso_id = a.idoso_id AND h.tipo = a.tipo
                           AND h.dia >= a.dia - 14 AND h.dia < a.dia) AS linha_base,
                       (SELECT COUNT(DISTINCT date_trunc('hour', m.medido_em, 'UTC'))
                          FROM medicao m WHERE m.idoso_id = a.idoso_id AND m.tipo = a.tipo
                           AND m.medido_em >= ? AND m.medido_em < ?) AS horas_com_dados
                  FROM agregado_diario a WHERE a.idoso_id = ? AND a.dia = ? AND a.tipo = ?
                """, (rs, linha) -> new TelemetriaIngerida.Agregado(chave.dia(), chave.tipo(),
                        rs.getString("unidade"), rs.getInt("quantidade"), rs.getDouble("minimo"),
                        rs.getDouble("maximo"), rs.getDouble("media"), rs.getDouble("soma"),
                        rs.getObject("linha_base", Double.class), rs.getInt("horas_com_dados")),
                inicio, fim, idosoId, chave.dia(), chave.tipo());
    }

    private record ChaveAgregado(LocalDate dia, String tipo) {
    }
}
