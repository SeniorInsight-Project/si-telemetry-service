# si-telemetry-service

Serviço de telemetria: provisiona o usuário no Open Wearables, lê a API dele a cada 15 min, descarta valores fora de faixa, agrega por dia, calcula a linha de base e detecta perda de sinal, bateria baixa e inatividade. Publica `telemetry.ingested`, `telemetry.signal.lost`, `telemetry.battery.low` e `telemetry.inactivity` na exchange `si.events`.

Parte do SeniorInsight — ver o Documento de Arquitetura de Software (seção 6) para as convenções.

Branches: `main` (produção) e `develop` (testes, branch padrão). Trabalhe na branch criada pelo card e abra PR para `develop` — ver [CONTRIBUTING.md](CONTRIBUTING.md).
