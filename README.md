# si-telemetry-service

Serviço de telemetria: provisiona o usuário no Open Wearables, lê a API dele a cada 15 min, descarta valores fora de faixa, agrega por dia, calcula a linha de base e detecta perda de sinal, bateria baixa e inatividade. Publica `telemetry.ingested`, `telemetry.signal.lost`, `telemetry.battery.low` e `telemetry.inactivity` na exchange `si.events`.

Parte do SeniorInsight — ver o Documento de Arquitetura de Software (seção 6) para as convenções.

Branches: `main` (produção) e `develop` (testes, branch padrão). Trabalhe na branch criada pelo card e abra PR para `develop` — ver [CONTRIBUTING.md](CONTRIBUTING.md).

## HU-027 — status do celular do idoso

O endpoint `GET /api/v1/telemetry/idosos/{idosoId}/status` retorna a última sincronização,
bateria, versão do aplicativo e tipos de dados ativos. A identidade é propagada pelo gateway
nos cabeçalhos `X-Authenticated-User-Id` e `X-Authenticated-Role`; o vínculo é confirmado
por chamada interna ao `care-service` antes da leitura.

O lote de telemetria aceita `battery_percentage`, `app_version` e `synced_at`. O evento
`consent.changed` atualiza a projeção local de `active_data_types`, portanto uma autorização
revogada aparece no status sem o telemetry-service acessar o banco do care-service.

## HU-028 — alerta de perda de sincronização

Um job verifica a projeção a cada 15 minutos. Por padrão, considera perda após 3 horas
sem lote entre 07:00 e 22:00 (`America/Sao_Paulo`); o limiar, a janela, o timezone e o
intervalo são configuráveis por `SYNC_MONITORING_*`. A perda é publicada uma única vez
por ciclo e o primeiro lote posterior publica o retorno. Os contratos CloudEvent v1 estão
em [`docs/contracts/telemetry-signal-v1.md`](docs/contracts/telemetry-signal-v1.md).
