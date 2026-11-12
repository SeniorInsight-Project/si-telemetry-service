package com.seniorinsight.telemetry.application.aparelho;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.entrada.ConsultarStatusAparelho;
import com.seniorinsight.telemetry.application.porta.entrada.StatusAparelho;
import com.seniorinsight.telemetry.application.porta.saida.ConsentimentosRede;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioStatusAparelho;
import com.seniorinsight.telemetry.application.porta.saida.RepositorioStatusAparelho.Registro;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.consentimento.TipoDado;
import org.springframework.stereotype.Service;

/**
 * Junta o que o aparelho informou (sincronização, bateria, app) com os tipos autorizados (projeção de
 * {@code consent.changed}). Idoso que ainda não sincronizou recebe os campos do aparelho {@code null}.
 */
@Service
class ConsultarStatusAparelhoService implements ConsultarStatusAparelho {

    private static final Set<Papel> LEITORES = Set.of(
            Papel.IDOSO, Papel.FAMILIAR_ADMINISTRADOR, Papel.FAMILIAR_APOIO);
    private static final Registro SEM_REGISTRO = new Registro(null, null, null, null, null);

    private final VerificarAcessoIdoso acesso;
    private final RepositorioStatusAparelho repositorio;
    private final ConsentimentosRede consentimentos;

    ConsultarStatusAparelhoService(VerificarAcessoIdoso acesso, RepositorioStatusAparelho repositorio,
                                   ConsentimentosRede consentimentos) {
        this.acesso = acesso;
        this.repositorio = repositorio;
        this.consentimentos = consentimentos;
    }

    @Override
    public StatusAparelho consultar(UUID idosoId, String sub) {
        acesso.exigir(idosoId, sub, LEITORES);
        Registro registro = repositorio.buscar(idosoId).orElse(SEM_REGISTRO);
        List<TipoDado> ativos = consentimentos.tiposAutorizados(idosoId).stream()
                .sorted(Comparator.naturalOrder()).toList();
        return new StatusAparelho(idosoId, registro.ultimaSincronizacao(), registro.nivelBateria(),
                registro.versaoApp(), registro.modeloCelular(), registro.sistemaOperacional(), ativos);
    }
}
