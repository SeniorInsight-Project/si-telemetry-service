package com.seniorinsight.telemetry.application.aparelho;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.seniorinsight.telemetry.application.acesso.VerificarAcessoIdoso;
import com.seniorinsight.telemetry.application.porta.saida.RegistroSincronizacaoAparelho;
import com.seniorinsight.telemetry.domain.acesso.Papel;
import org.springframework.stereotype.Service;

@Service
public class RegistrarStatusAparelho {
    private final VerificarAcessoIdoso acesso;
    private final RegistroSincronizacaoAparelho repositorio;

    public RegistrarStatusAparelho(VerificarAcessoIdoso acesso, RegistroSincronizacaoAparelho repositorio) {
        this.acesso = acesso;
        this.repositorio = repositorio;
    }

    public void registrar(String sub, Comando comando) {
        acesso.exigir(comando.idosoId(), sub, Set.of(Papel.IDOSO));
        repositorio.registrarStatus(comando.idosoId(), comando.sincronizadoEm(), comando.nivelBateria(),
                comando.versaoApp(), comando.modeloCelular(), comando.sistemaOperacional());
    }

    public record Comando(UUID idosoId, Instant sincronizadoEm, Integer nivelBateria, String versaoApp,
                          String modeloCelular, String sistemaOperacional) {
    }
}
