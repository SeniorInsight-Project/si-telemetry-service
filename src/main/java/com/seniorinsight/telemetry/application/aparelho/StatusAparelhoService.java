package com.seniorinsight.telemetry.application.aparelho;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.saida.StatusAparelhoRepositorio;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import com.seniorinsight.telemetry.domain.aparelho.StatusAparelho;
import org.springframework.stereotype.Service;

@Service
public class StatusAparelhoService {

    private static final Set<Papel> LEITORES = Set.of(
            Papel.IDOSO, Papel.FAMILIAR_ADMINISTRADOR, Papel.FAMILIAR_APOIO);

    private final VerificarAcessoIdoso acesso;
    private final StatusAparelhoRepositorio repositorio;

    public StatusAparelhoService(VerificarAcessoIdoso acesso, StatusAparelhoRepositorio repositorio) {
        this.acesso = acesso;
        this.repositorio = repositorio;
    }

    public Optional<StatusAparelho> consultar(UUID idosoId, String sub) {
        acesso.exigir(idosoId, sub, LEITORES);
        return repositorio.buscar(idosoId);
    }

    public void registrar(UUID idosoId, String sub, Instant sincronizadoEm, Integer nivelBateria,
                          String versaoApp, String modeloCelular, String sistemaOperacional) {
        acesso.exigir(idosoId, sub, Set.of(Papel.IDOSO));
        repositorio.registrar(idosoId, sincronizadoEm, nivelBateria, versaoApp, modeloCelular, sistemaOperacional);
    }
}
