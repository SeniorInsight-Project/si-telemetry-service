package com.seniorinsight.telemetry.application.lgpd;

import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.ExclusaoDadosRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.domain.evento.ExclusaoConcluida;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

@Component
public class TratarExclusao {

    private final ExclusaoDadosRepositorio repositorio;
    private final PublicadorEventos eventos;
    private final OpenWearables provedor;
    private final TransactionOperations transacoes;

    public TratarExclusao(ExclusaoDadosRepositorio repositorio, PublicadorEventos eventos,
                          OpenWearables provedor, TransactionOperations transacoes) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.provedor = provedor;
        this.transacoes = transacoes;
    }

    public void tratar(Pedido pedido) {
        if (repositorio.confirmado(pedido.protocolo())) {
            return;
        }
        if (pedido.idoso()) {
            var usuario = transacoes.execute(status -> {
                var vinculo = repositorio.bloquearEBuscarUsuarioOpenWearables(pedido.titular());
                repositorio.bloquearParaExclusao(pedido.titular());
                return vinculo;
            });
            usuario.ifPresent(provedor::excluirUsuario);
        }
        transacoes.executeWithoutResult(status -> {
            if (repositorio.reservar(pedido.protocolo())) {
                if (pedido.idoso()) {
                    repositorio.excluirEAnonimizar(pedido.titular());
                }
                eventos.publicar(new ExclusaoConcluida(pedido.protocolo(), "telemetry-service"));
            }
        });
    }


    public record Pedido(UUID protocolo, UUID titular, String sub, boolean idoso) {
    }
}
