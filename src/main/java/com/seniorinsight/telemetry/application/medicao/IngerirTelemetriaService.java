package com.seniorinsight.telemetry.application.medicao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.entrada.IngerirTelemetria;
import com.seniorinsight.telemetry.application.porta.entrada.ResultadoIngestao;
import com.seniorinsight.telemetry.application.porta.entrada.TelemetriaRecebida;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.application.porta.saida.RegistroSincronizacaoAparelho;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioMedicoes;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.evento.QuedaDetectada;
import com.seniorinsight.telemetry.domain.medicao.Leitura;
import com.seniorinsight.telemetry.domain.medicao.TipoMedicao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transforma cada leitura consolidada em amostras por sinal, descarta o que está fora da faixa ou no futuro, grava
 * as novas e, na mesma transação, atualiza o status do aparelho e enfileira o evento de queda (outbox).
 */
@Service
class IngerirTelemetriaService implements IngerirTelemetria {

    /** Tolerância para relógio do celular adiantado. */
    static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);
    private static final String ORIGEM_PADRAO = "app";
    private static final int TAMANHO_MAXIMO_ORIGEM = 100;

    private final VerificarAcessoIdoso acesso;
    private final RepositorioMedicoes medicoes;
    private final RegistroSincronizacaoAparelho sincronizacao;
    private final PublicadorEventos publicador;
    private final Clock relogio;

    @Autowired
    IngerirTelemetriaService(VerificarAcessoIdoso acesso, RepositorioMedicoes medicoes,
                             RegistroSincronizacaoAparelho sincronizacao, PublicadorEventos publicador) {
        this(acesso, medicoes, sincronizacao, publicador, Clock.systemUTC());
    }

    IngerirTelemetriaService(VerificarAcessoIdoso acesso, RepositorioMedicoes medicoes,
                             RegistroSincronizacaoAparelho sincronizacao, PublicadorEventos publicador,
                             Clock relogio) {
        this.acesso = acesso;
        this.medicoes = medicoes;
        this.sincronizacao = sincronizacao;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public ResultadoIngestao ingerir(UUID idosoId, String sub, List<TelemetriaRecebida> leituras) {
        acesso.exigir(idosoId, sub, Set.of(Papel.IDOSO));

        Instant limite = relogio.instant().plus(TOLERANCIA_FUTURO);
        List<Leitura> amostras = new ArrayList<>();
        int descartadas = 0;
        Instant ultimaSincronizacao = null;
        Integer bateria = null;

        for (TelemetriaRecebida leitura : leituras) {
            if (leitura.medidaEm() == null || leitura.medidaEm().isAfter(limite)) {
                descartadas += Math.max(1, contarSinais(leitura));
                continue;
            }
            String origem = origem(leitura.provedor());
            descartadas += adicionar(amostras, TipoMedicao.FREQUENCIA_CARDIACA, leitura.medidaEm(), leitura.batimentos(), origem);
            descartadas += adicionar(amostras, TipoMedicao.FREQUENCIA_CARDIACA_REPOUSO, leitura.medidaEm(), leitura.batimentosRepouso(), origem);
            descartadas += adicionar(amostras, TipoMedicao.VARIABILIDADE_CARDIACA, leitura.medidaEm(), leitura.variabilidadeCardiacaMs(), origem);
            descartadas += adicionar(amostras, TipoMedicao.PASSOS, leitura.medidaEm(), leitura.passos(), origem);
            descartadas += adicionar(amostras, TipoMedicao.DISTANCIA, leitura.medidaEm(), leitura.distanciaMetros(), origem);
            descartadas += adicionar(amostras, TipoMedicao.CALORIAS_ATIVAS, leitura.medidaEm(), leitura.caloriasAtivasKcal(), origem);
            descartadas += adicionar(amostras, TipoMedicao.OXIGENACAO, leitura.medidaEm(), leitura.oxigenacaoPercentual(), origem);

            if (Boolean.TRUE.equals(leitura.quedaDetectada())) {
                publicador.publicar(new QuedaDetectada(idosoId, instanteDaQueda(leitura, limite),
                        leitura.forcaImpactoG(), leitura.confiancaQueda()));
            }
            if (ultimaSincronizacao == null || leitura.medidaEm().isAfter(ultimaSincronizacao)) {
                ultimaSincronizacao = leitura.medidaEm();
                bateria = bateriaValida(leitura.nivelBateria());
            }
        }

        int gravadas = medicoes.gravarNovas(idosoId, amostras);
        if (ultimaSincronizacao != null) {
            sincronizacao.registrar(idosoId, ultimaSincronizacao, bateria);
        }
        return new ResultadoIngestao(leituras.size(), gravadas, descartadas);
    }

    /** Soma 1 se o sinal veio mas foi recusado; não soma nada se o sinal não veio. */
    private static int adicionar(List<Leitura> amostras, TipoMedicao tipo, Instant medidaEm, Number valor,
                                 String origem) {
        if (valor == null) {
            return 0;
        }
        if (!tipo.valorValido(valor.doubleValue())) {
            return 1;
        }
        amostras.add(tipo.leitura(medidaEm, valor.doubleValue(), origem));
        return 0;
    }

    private static int contarSinais(TelemetriaRecebida l) {
        return (int) Stream.of(l.batimentos(), l.batimentosRepouso(), l.variabilidadeCardiacaMs(),
                l.passos(), l.distanciaMetros(), l.caloriasAtivasKcal(), l.oxigenacaoPercentual())
                .filter(Objects::nonNull).count();
    }

    private static Instant instanteDaQueda(TelemetriaRecebida leitura, Instant limite) {
        Instant queda = leitura.quedaEm();
        return queda != null && !queda.isAfter(limite) ? queda : leitura.medidaEm();
    }

    private static Integer bateriaValida(Integer nivel) {
        return nivel != null && nivel >= 0 && nivel <= 100 ? nivel : null;
    }

    private static String origem(String provedor) {
        if (provedor == null || provedor.isBlank()) {
            return ORIGEM_PADRAO;
        }
        String limpo = provedor.strip();
        return limpo.length() > TAMANHO_MAXIMO_ORIGEM ? limpo.substring(0, TAMANHO_MAXIMO_ORIGEM) : limpo;
    }
}
