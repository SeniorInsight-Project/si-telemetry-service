# Eventos de perda e retorno do sinal

O telemetry-service grava o evento e a mudança no estado do aparelho na
mesma transação. A outbox publica na exchange `si.events`. O identificador
para deduplicação é o `message_id` do RabbitMQ.

| Routing key | Campos do corpo JSON |
|---|---|
| `telemetry.signal.lost` | `idosoId`, `ultimaSincronizacao`, `detectadoEm` |
| `telemetry.signal.recovered` | `idosoId`, `sincronizadoEm` |

Os campos de horário são instantes ISO 8601. O monitor emite uma perda por
ciclo, quando o aparelho tem uma sincronização anterior, ultrapassou a
tolerância e a hora local está na janela diurna. O retorno é emitido quando
o aparelho registra uma sincronização posterior à detecção.

Este formato usa a serialização atual da outbox do telemetry-service. O
contrato compartilhado e o consumo pelo notification-service precisam ser
alinhados antes da integração ponta a ponta.
