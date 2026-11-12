package com.seniorinsight.telemetry.application.medicao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.entrada.ConsultarEstadoAtual;
import com.seniorinsight.telemetry.application.porta.entrada.EstadoAtual;
import com.seniorinsight.telemetry.application.porta.saida.CacheMedicoesHoje;
import com.seniorinsight.telemetry.application.porta.saida.ConsentimentosRede;
import com.seniorinsight.telemetry.application.porta.saida.ConsultaMedicoesHoje;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.consentimento.TipoDado;
import com.seniorinsight.telemetry.domain.medicao.MedicoesHoje;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Monta o estado de hoje só com os tipos que o idoso autoriza para a rede. O consentimento é lido a cada consulta e
 * faz parte da chave do cache, então uma revogação vale na hora e nunca devolve valor antigo (HU-019). Sem leitura
 * nas últimas 48 h, os valores vêm {@code null} (RN-003).
 */
@Service
class ConsultarEstadoAtualService implements ConsultarEstadoAtual {

    static final Duration LIMITE_RECENCIA = Duration.ofHours(48);
    private static final Set<Papel> LEITORES = Set.of(
            Papel.IDOSO, Papel.FAMILIAR_ADMINISTRADOR, Papel.FAMILIAR_APOIO);

    private final VerificarAcessoIdoso acesso;
    private final ConsentimentosRede consentimentos;
    private final ConsultaMedicoesHoje medicoes;
    private final CacheMedicoesHoje cache;
    private final Clock relogio;
    private final ZoneId fuso;
    private final int metaPassos;

    @Autowired
    ConsultarEstadoAtualService(VerificarAcessoIdoso acesso, ConsentimentosRede consentimentos,
                                ConsultaMedicoesHoje medicoes, CacheMedicoesHoje cache,
                                @Value("${telemetry.hoje.fuso}") String fuso,
                                @Value("${telemetry.hoje.meta-passos}") int metaPassos) {
        this(acesso, consentimentos, medicoes, cache, Clock.systemUTC(), ZoneId.of(fuso), metaPassos);
    }

    ConsultarEstadoAtualService(VerificarAcessoIdoso acesso, ConsentimentosRede consentimentos,
                                ConsultaMedicoesHoje medicoes, CacheMedicoesHoje cache, Clock relogio, ZoneId fuso,
                                int metaPassos) {
        if (metaPassos < 0) {
            throw new IllegalArgumentException("telemetry.hoje.meta-passos deve ser maior ou igual a zero");
        }
        this.acesso = acesso;
        this.consentimentos = consentimentos;
        this.medicoes = medicoes;
        this.cache = cache;
        this.relogio = relogio;
        this.fuso = fuso;
        this.metaPassos = metaPassos;
    }

    @Override
    public EstadoAtual consultar(UUID idosoId, String sub) {
        acesso.exigir(idosoId, sub, LEITORES);

        Set<TipoDado> autorizados = consentimentos.tiposAutorizados(idosoId);
        boolean passos = autorizados.contains(TipoDado.PASSOS);
        boolean batimentos = autorizados.contains(TipoDado.BATIMENTOS);
        if (!passos && !batimentos) {
            return new EstadoAtual(idosoId, null, metaPassos, null, null, null);
        }

        Instant agora = relogio.instant();
        LocalDate dia = agora.atZone(fuso).toLocalDate();
        String chave = "telemetry:hoje:v1:" + idosoId + ":" + dia + ":" + passos + ":" + batimentos;

        MedicoesHoje leitura = cache.buscar(chave).orElseGet(() -> {
            MedicoesHoje lida = medicoes.buscar(idosoId, dia, agora, passos, batimentos);
            // Só guarda o que é recente: o primeiro dado de quem estava sem sinal não fica 15 min escondido.
            if (recente(lida.coletadoEm(), agora)) {
                cache.guardar(chave, lida);
            }
            return lida;
        });

        if (!recente(leitura.coletadoEm(), agora)) {
            return new EstadoAtual(idosoId, null, metaPassos, null, null, leitura.coletadoEm());
        }
        return new EstadoAtual(idosoId,
                passos ? leitura.passos() : null, metaPassos,
                batimentos ? leitura.frequenciaCardiaca() : null,
                batimentos ? leitura.frequenciaCardiacaRepouso() : null,
                leitura.coletadoEm());
    }

    private static boolean recente(Instant coletadoEm, Instant agora) {
        return coletadoEm != null && !coletadoEm.isBefore(agora.minus(LIMITE_RECENCIA));
    }
}
