# Contrato de eventos de sinal — v1

O repositório não contém o repositório compartilhado `si-contracts` nem um adaptador RabbitMQ implementado. Até essa integração existir, o `si-telemetry-service` publica estes CloudEvents pelo `ApplicationEventPublisher`; a borda de mensageria deve serializar o registro sem alterar seus campos.

## `telemetry.signal.lost.v1`

Emitido uma única vez por idoso quando `last_sync_at + threshold <= detected_at` durante a janela configurada (padrão 07:00–22:00, `America/Sao_Paulo`). O estado persistido impede duplicidade até o retorno.

```json
{
  "specversion": "1.0",
  "id": "uuid",
  "source": "/si-telemetry-service",
  "type": "telemetry.signal.lost.v1",
  "time": "2026-11-01T15:00:00Z",
  "subject": "idoso-id",
  "data": {
    "idoso_id": "uuid",
    "last_sync_at": "2026-11-01T12:00:00Z",
    "detected_at": "2026-11-01T15:00:00Z"
  }
}
```

Routing key alvo: `telemetry.signal.lost`.

## `telemetry.signal.recovered.v1`

Emitido quando chega o primeiro lote após uma perda registrada. O estado é rearmado antes da publicação, permitindo um novo ciclo de perda.

```json
{
  "specversion": "1.0",
  "id": "uuid",
  "source": "/si-telemetry-service",
  "type": "telemetry.signal.recovered.v1",
  "time": "2026-11-01T16:15:00Z",
  "subject": "idoso-id",
  "data": {
    "idoso_id": "uuid",
    "synchronized_at": "2026-11-01T16:15:00Z"
  }
}
```

Routing key alvo: `telemetry.signal.recovered`.

Consumidores devem usar `id` para idempotência. `idoso_id` é um identificador interno; os logs não devem registrar os dados do evento.
