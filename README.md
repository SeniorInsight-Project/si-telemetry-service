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
