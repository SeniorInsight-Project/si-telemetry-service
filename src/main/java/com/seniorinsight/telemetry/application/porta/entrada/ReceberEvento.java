package com.seniorinsight.telemetry.application.porta.entrada;

/**
 * Processa um evento recebido de outro serviço. Idempotente pelo id do evento: a mesma mensagem entregue de novo
 * não é processada outra vez.
 *
 * <p>Se lançar exceção, nada do processamento fica gravado e o evento pode ser reprocessado.
 */
public interface ReceberEvento {

    void receber(EventoRecebido evento);
}
