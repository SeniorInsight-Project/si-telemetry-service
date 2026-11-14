package com.seniorinsight.telemetry.application.lgpd;

import java.util.UUID;

import com.seniorinsight.telemetry.application.porta.saida.OpenWearables;
import com.seniorinsight.telemetry.application.porta.saida.ExclusaoDadosRepositorio;
import com.seniorinsight.telemetry.application.porta.saida.PublicadorEventos;
import com.seniorinsight.telemetry.application.porta.saida.VinculosOpenWearables;
import com.seniorinsight.telemetry.application.medicao.ReferenciaOpenWearables;
import com.seniorinsight.telemetry.domain.evento.ExclusaoConcluida;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

@Component
public class TratarExclusao {

    private final ExclusaoDadosRepositorio repositorio;
    private final PublicadorEventos eventos;
    private final OpenWearables provedor;
    private final TransactionOperations transacoes;
    private final VinculosOpenWearables vinculos;
    private final ReferenciaOpenWearables referencias;

    public TratarExclusao(ExclusaoDadosRepositorio repositorio, PublicadorEventos eventos,
                          OpenWearables provedor, TransactionOperations transacoes,
                          VinculosOpenWearables vinculos, ReferenciaOpenWearables referencias) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.provedor = provedor;
        this.transacoes = transacoes;
        this.vinculos = vinculos;
        this.referencias = referencias;
    }

    public void tratar(Pedido pedido) {
        if (pedido.idoso()) {
            if (!vinculos.executarExclusivo(pedido.titular(), () -> processar(pedido))) {
                throw new IllegalStateException("Open Wearables em uso; tente novamente");
            }
        } else {
            processar(pedido);
        }
    }

    private void processar(Pedido pedido) {
        if (repositorio.confirmado(pedido.protocolo())) {
            return;
        }
        if (pedido.idoso()) {
            var usuario = transacoes.execute(status -> {
                var vinculo = repositorio.bloquearEBuscarUsuarioOpenWearables(pedido.titular());
                repositorio.bloquearParaExclusao(pedido.titular());
                return vinculo;
            });
            var externo = usuario.or(() -> referencias.gerar(pedido.titular())
                    .flatMap(provedor::buscarUsuarioPorReferencia));
            if (externo.isEmpty() && vinculos.temDadosLegados(pedido.titular())) {
                throw new IllegalStateException("Exclusão aguardando a reconciliação do vínculo com o Open Wearables");
            }
            externo.ifPresent(provedor::excluirUsuario);
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
