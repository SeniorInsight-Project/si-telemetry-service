package com.seniorinsight.telemetry.application.medicao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.saida.CacheMedicoesHoje;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaMedicoesHoje;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.aparelho.TipoDado;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConsultarEstadoAtual {

    private static final Set<Papel> LEITORES = Set.of(
            Papel.IDOSO, Papel.FAMILIAR_ADMINISTRADOR, Papel.FAMILIAR_APOIO);
    private static final Duration LIMITE_RECENCIA = Duration.ofHours(48);

    private final VerificarAcessoIdoso acesso;
    private final StatusAparelhoRepositorio consentimentos;
    private final ConsultaMedicoesHoje medicoes;
    private final CacheMedicoesHoje cache;
    private final Clock relogio;
    private final ZoneId fuso;
    private final int metaPassos;

    public ConsultarEstadoAtual(VerificarAcessoIdoso acesso, StatusAparelhoRepositorio consentimentos,
            ConsultaMedicoesHoje medicoes, CacheMedicoesHoje cache, Clock relogio,
            @Value("${telemetry.hoje.fuso}") String fuso, @Value("${telemetry.hoje.meta-passos}") int metaPassos) {
        this.acesso = acesso;
        this.consentimentos = consentimentos;
        this.medicoes = medicoes;
        this.cache = cache;
        this.relogio = relogio;
        this.fuso = ZoneId.of(fuso);
        if (metaPassos < 0) {
            throw new IllegalArgumentException("telemetry.hoje.meta-passos deve ser maior ou igual a zero");
        }
        this.metaPassos = metaPassos;
    }

    public EstadoAtual consultar(UUID idosoId, String sub) {
        acesso.exigir(idosoId, sub, LEITORES);
        var tipos = consentimentos.buscarTiposAtivosRede(idosoId);
        boolean passos = tipos.contains(TipoDado.PASSOS);
        boolean batimentos = tipos.contains(TipoDado.BATIMENTOS);
        if (!passos && !batimentos) {
            return new EstadoAtual(idosoId, null, metaPassos, null, null, null);
        }

        Instant agora = relogio.instant();
        var dia = agora.atZone(fuso).toLocalDate();
        String chave = "telemetry:hoje:v1:" + idosoId + ":" + dia + ":" + passos + ":" + batimentos;
        MedicoesHoje leitura = cache.buscar(chave).orElseGet(() -> {
            MedicoesHoje atual = medicoes.buscar(idosoId, dia, dia.atStartOfDay(fuso).toInstant(),
                    agora, passos, batimentos);
            if (recente(atual.coletadoEm(), agora)) {
                cache.guardar(chave, atual);
            }
            return atual;
        });

        boolean recente = recente(leitura.coletadoEm(), agora);
        return new EstadoAtual(idosoId, passos && recente ? leitura.passos() : null, metaPassos,
                batimentos && recente ? leitura.frequenciaCardiaca() : null,
                batimentos && recente ? leitura.frequenciaCardiacaRepouso() : null, leitura.coletadoEm());
    }

    private static boolean recente(Instant coletadoEm, Instant agora) {
        return coletadoEm != null && !coletadoEm.isBefore(agora.minus(LIMITE_RECENCIA));
    }

    public record EstadoAtual(UUID idosoId, Long passosHoje, int metaPassos, Integer frequenciaCardiaca,
                              Integer frequenciaCardiacaRepouso, Instant coletadoEm) {
    }
}
