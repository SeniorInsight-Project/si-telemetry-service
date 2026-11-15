# si-telemetry-service

Serviço de telemetria: provisiona o usuário no Open Wearables, lê a API dele a cada 15 min, descarta valores fora de faixa, agrega por dia, calcula a linha de base e detecta perda de sinal, bateria baixa e inatividade. Publica `telemetry.ingested`, `telemetry.signal.lost`, `telemetry.battery.low` e `telemetry.inactivity` na exchange `si.events`.

A perda e o retorno do sinal são publicados como CloudEvents em
`telemetry.signal.lost` e `telemetry.signal.recovered`. Os schemas v1 ficam
no repositório `si-contracts`. O `id` do CloudEvent é o mesmo `message_id`
do RabbitMQ e permanece estável nas reentregas.

Parte do SeniorInsight — ver o Documento de Arquitetura de Software (seção 6) para as convenções.

Branches: `main` (produção) e `develop` (testes, branch padrão). Trabalhe na branch criada pelo card e abra PR para `develop` — ver [CONTRIBUTING.md](CONTRIBUTING.md).

## Stack

Java 21, Spring Boot 3.5, PostgreSQL (schema `telemetry`, migrações com Flyway), RabbitMQ (exchange topic `si.events` e fila `telemetry.q`), Resilience4j no cliente do Open Wearables e Keycloak como emissor dos tokens (o serviço é só resource server).

## Organização do código

Pacote base `com.seniorinsight.telemetry`, em arquitetura hexagonal (mesmo padrão do si-identity-service):

| Pacote | O que tem | Pode depender de |
|---|---|---|
| `domain` | Modelo e eventos de domínio (`acesso.Papel`, `medicao.Leitura`, `evento.EventoDominio`), Java puro | nada (nem Spring, nem JPA) |
| `application` | Casos de uso (`acesso.VerificarAcessoIdoso`, `evento.ReceberEventoService`) e portas (`porta.entrada`, `porta.saida`) | `domain` |
| `adapters.in` | Entrada: HTTP, segurança, OpenAPI (`web`) e consumidor da fila (`mensageria`) | `application`, `domain` |
| `adapters.out` | Saída: outbox (`outbox`), banco (`persistencia`), Open Wearables (`openwearables`) e care-service (`care`) | `application`, `domain` |

`adapters.in` e `adapters.out` não se conhecem. As regras são verificadas pelo `ArquiteturaTest` (ArchUnit) a cada build.

O código de telemetria que vem do si-care-service (si-telemetry-service#1) entra nesse desenho: entidade JPA e repositório em `adapters.out.persistencia` (gravando em `medicao`), controller e DTOs em `adapters.in.web`, regras em `application`/`domain`, eventos como `EventoDominio` publicados pela porta `PublicadorEventos` e chamadas ao Open Wearables pela porta `OpenWearables`. A migração dele passa a ser a `V2__...`.

## Rodando localmente

Pré-requisitos: JDK 21, Maven 3.9+ e Docker.

### Sem infraestrutura própria (Testcontainers)

Sobe o serviço com PostgreSQL e RabbitMQ em containers descartáveis:

```bash
KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/seniorinsight mvn spring-boot:test-run
```

Sem um Keycloak no ar o serviço sobe normalmente: o health e o Swagger funcionam, e as rotas protegidas respondem 401.

### Com PostgreSQL, RabbitMQ e Keycloak já rodando

```bash
cp .env.example .env   # preencha as senhas
set -a && source .env && set +a
mvn spring-boot:run
```

O usuário do banco precisa poder criar o schema `telemetry` (ou o schema precisa existir com ele como dono): o Flyway cria o schema e aplica as migrações na subida.

### Endpoints

| Rota | Acesso |
|---|---|
| `GET /actuator/health` (e `/liveness`, `/readiness`) | público |
| `/swagger-ui.html` e `/v3/api-docs` | público |
| `POST /v1/idosos/{idosoId}/telemetria` e `.../telemetria/lote` | token do próprio idoso |
| todo o resto (as demais rotas `/api/v1/telemetry/...` chegam nas próximas issues) | token do Keycloak |

O gateway faz `StripPrefix=3` + `PrefixPath=/v1`: `/api/v1/telemetry/idosos/{id}/telemetria` chega aqui como `/v1/idosos/{id}/telemetria`. O controller mapeia sempre `/v1/idosos/...`.

## Variáveis de ambiente

O `application.yml` não tem segredos; tudo vem do ambiente. O modelo está no [`.env.example`](.env.example).

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `seniorinsight` | Banco (cada serviço usa o seu schema) |
| `DB_USERNAME` | — | Usuário do banco |
| `DB_PASSWORD` | — | Senha do banco |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta AMQP |
| `RABBITMQ_VHOST` | `/` | Virtual host |
| `RABBITMQ_USERNAME` | — | Usuário do RabbitMQ |
| `RABBITMQ_PASSWORD` | — | Senha do RabbitMQ |
| `KEYCLOAK_ISSUER_URI` | — | Issuer do realm, ex.: `http://localhost:8180/realms/seniorinsight` |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWKSETURI` | — | Opcional: URL das chaves públicas quando o issuer não é acessível de dentro do serviço (Docker) |
| `OPENWEARABLES_ENABLED` | `false` | Liga a leitura periódica do Open Wearables (si-telemetry-service#5) |
| `OPENWEARABLES_BASE_URL` | `http://localhost:8000` | Endereço da API do Open Wearables (self-hosted) |
| `OPENWEARABLES_API_KEY` | — | Chave da API, enviada no header `X-Open-Wearables-API-Key` |
| `CARE_SERVICE_URL` | `http://localhost:8082` | Endereço do care-service, para a verificação de vínculo (RF-007) |
| `TELEMETRY_REDIS_ENABLED` | `false` | Liga o cache de 15 min da rota de hoje no Redis; desligado, a rota lê do PostgreSQL |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Endereço do Redis, usado só com o cache ligado |
| `TELEMETRY_META_PASSOS` | `4000` | Meta diária de passos devolvida em `metaPassos` |
| `SERVER_PORT` | `8080` | Porta HTTP |

`KEYCLOAK_ISSUER_URI` precisa ser **exatamente** o valor do claim `iss` dos tokens. No Compose o issuer é `http://localhost:8180/...`, que não é acessível de dentro da rede dos containers; por isso as chaves são baixadas de `http://keycloak:8080/.../certs` via `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWKSETURI`, e o `iss` continua sendo validado contra o issuer.

Ajustes opcionais (valores padrão no `application.yml`): relay da outbox (`TELEMETRY_OUTBOX_RELAY_INTERVALO`, `TELEMETRY_OUTBOX_RELAY_TAMANHO_LOTE`, `TELEMETRY_OUTBOX_RELAY_TIMEOUT_CONFIRMACAO`), prefetch da fila (`TELEMETRY_MENSAGERIA_PREFETCH`), partições (`TELEMETRY_PARTICOES_MESES_A_FRENTE`, `TELEMETRY_PARTICOES_CRON`) e timeouts dos clientes HTTP (`TELEMETRY_OPEN_WEARABLES_TIMEOUT_CONEXAO`, `TELEMETRY_OPEN_WEARABLES_TIMEOUT_LEITURA`, `TELEMETRY_CARE_SERVICE_TIMEOUT_CONEXAO`, `TELEMETRY_CARE_SERVICE_TIMEOUT_LEITURA`).

## Segurança

O serviço não tem login nem emite tokens: valida o JWT do Keycloak pelo issuer, sem sessão (stateless). As roles de `realm_access.roles` viram authorities `ROLE_<role>`, com o nome da role como está no realm (ex.: `familiar` → `ROLE_familiar`); os scopes do token viram `SCOPE_<scope>`.

### Papel e vínculo (RF-007)

Ter um token válido não basta para ver os dados de um idoso: toda rota com `idosoId` chama `VerificarAcessoIdoso.exigir(idosoId, sub, papéisPermitidos)`. O papel vem do endpoint interno do care-service `GET /internal/idosos/{idosoId}/vinculo/{sub}` (si-care-service#74), atrás da porta `ConsultaVinculo`:

- 200 com `{"papel": "FAMILIAR_ADMINISTRADOR" | "FAMILIAR_APOIO" | "MEDICO" | "IDOSO"}`: há vínculo;
- 404: não há vínculo → `403 Forbidden`;
- erro ou timeout: `503 Service Unavailable` (o acesso não é liberado sem resposta do care-service).

O token do usuário é repassado na chamada para o care-service autenticá-la.

## Banco (schema `telemetry`)

| Tabela | Para quê |
|---|---|
| `medicao` | Amostras dos wearables, particionada por mês de `medido_em` (UTC) em `medicao_AAAA_MM`, com a partição padrão `medicao_padrao` para o resto. `UNIQUE (idoso_id, tipo, medido_em, origem)` deduplica as leituras (RF-019) |
| `agregado_diario` | Resumo por idoso, dia e tipo de sinal (RF-025): quantidade, mínimo, máximo, média e soma |
| `status_aparelho` | Último status do celular do idoso (bateria, versão do app, horário — RF-020), última sincronização e início da perda de sinal |
| `outbox_evento` | Eventos a publicar na `si.events` |
| `evento_recebido` | Ids dos eventos já processados da `telemetry.q` (idempotência) |

A função `criar_particao_medicao(mes)` cria a partição de um mês se ela não existir. A migração cria o mês atual e os dois seguintes, e o serviço (`ManutencaoParticoesMedicao`) garante a mesma janela na subida e todo dia às 03:00 UTC.

## Eventos

### Publicação (outbox)

Igual ao si-identity-service: quem precisa publicar chama a porta `PublicadorEventos` dentro da transação do caso de uso; o evento é gravado em `outbox_evento` e o `RelayOutbox` publica na exchange topic `si.events` com routing key = tipo do evento, `message_id` = id da linha na outbox e publisher confirms. A entrega é "pelo menos uma vez".

### Consumo (fila `telemetry.q`)

| Item | Valor |
|---|---|
| Fila | `telemetry.q` (durável), ligada à `si.events` pelas routing keys `consent.changed` e `lgpd.erasure.requested` |
| Dead-letter | `telemetry.dlx` (direct) → `telemetry.q.dlq` |
| Ack | manual, depois de a transação do processamento ser gravada; prefetch 10 |
| Idempotência | o `message_id` é gravado em `evento_recebido` na mesma transação do tratamento; mensagem repetida é confirmada sem tratar de novo |
| Falha | mensagem sem `message_id` válido ou exceção no tratamento: reject sem requeue → dead-letter, e nada do processamento fica gravado |

Para tratar um novo tipo de evento, crie um bean `TratadorEvento` (em `application`) com a routing key e, se for preciso, inclua a routing key em `MensageriaConfig.EVENTOS_ASSINADOS`. Os tratadores de `consent.changed` e `lgpd.erasure.requested` chegam nas si-telemetry-service#3 e #4.

## Open Wearables

O cliente fica em `adapters.out.openwearables` e implementa a porta `OpenWearables` (`provisionarUsuario` e `buscarLeituras`), sobre a API REST do Open Wearables (`POST /api/v1/users` e `GET /api/v1/users/{id}/timeseries`, paginada por cursor). Cada requisição tem:

- timeout de conexão (2 s) e de leitura (10 s);
- retry (instância `openWearables`): até 3 tentativas com backoff exponencial a partir de 0,5 s, só em falha de rede/timeout e 5xx;
- circuit breaker (instância `openWearables`): abre com 50% de falha em 10 chamadas (mínimo 5) e fica aberto 30 s.

Falha depois disso (ou circuito aberto) chega como `OpenWearablesIndisponivelException`. Os parâmetros ficam em `resilience4j.*` no `application.yml`.

## Ingestão de telemetria (si-telemetry-service#1)

Substitui a ingestão que ficava no care-service (`TelemetriaWearable`, `WearableIntegrationService`). O app do idoso envia o mesmo corpo em snake_case (`heart_rate`, `steps`, `fall_detection`, `battery_level`, `spo2_percentage`) para `POST /v1/idosos/{idosoId}/telemetria` (uma leitura) ou `.../telemetria/lote` (até 500). Só o próprio idoso pode enviar (RF-007) e a resposta é `202` com `{recebidas, gravadas, descartadas}`.

| Origem no payload | Tipo gravado em `medicao` | Unidade | Faixa aceita |
|---|---|---|---|
| `heart_rate.bpm` | `heart_rate` | bpm | 30–260 |
| `heart_rate.resting_bpm` | `resting_heart_rate` | bpm | 30–200 |
| `heart_rate.hrv_ms` | `hrv` | ms | 0–500 |
| `steps.count` | `steps` | passos | 0–200000 |
| `steps.distance_meters` | `distance` | m | 0–500000 |
| `steps.calories_kcal` | `active_calories` | kcal | 0–20000 |
| `spo2_percentage` | `oxygen_saturation` | % | 50–100 |

- Valor fora da faixa ou horário mais de 5 minutos no futuro: a amostra é descartada e contada em `descartadas`; o resto do envio é gravado.
- Deduplicação por idoso, tipo, horário e origem (RF-019; origem = `provider`, ou `app` se ausente). O `agregado_diario` só soma amostras novas, então reenviar não infla os passos. `steps.count` é tratado como passos da janela da leitura e somado no dia.
- O dia do resumo usa o fuso `TELEMETRY_HOJE_FUSO` (padrão `America/Sao_Paulo`).
- A leitura mais recente atualiza `status_aparelho` (última sincronização e bateria) e limpa `sinal_perdido_desde`.
- `fall_detection.fall_detected = true` publica `telemetry.fall.detected` pela outbox.
- A rota antiga do care-service continua no ar; ela só é removida depois da troca do app.

## Estado de hoje

`GET /v1/idosos/{idosoId}/hoje` (pelo gateway, `/api/v1/telemetry/idosos/{idosoId}/hoje`) devolve o `EstadoAtual` do contrato `telemetry-v1`: `passosHoje`, `metaPassos`, `frequenciaCardiaca`, `frequenciaCardiacaRepouso` e `coletadoEm`. Quem lê: o próprio idoso e a rede de apoio vinculada no care-service; os demais recebem 403.

- Só entram os tipos que o idoso autoriza para a rede. O consentimento é a projeção de `consent.changed` (destinatário `REDE`) na tabela `consentimento_rede`; um tipo revogado vem `null`, nunca com o valor antigo.
- Sem leitura nas últimas 48 h, os valores vêm `null` (sem dados recentes, RN-003).
- Com `TELEMETRY_REDIS_ENABLED=true`, as medições do dia ficam 15 min no Redis (RNF-002). A chave inclui o consentimento, então uma revogação vale na hora. Com o Redis fora do ar, a rota segue pelo PostgreSQL.

## Status do celular

`GET /v1/idosos/{idosoId}/status` (pelo gateway, `/api/v1/telemetry/idosos/{idosoId}/status`) devolve o `StatusAparelho` do contrato: `ultimaSincronizacao`, `nivelBateria`, `versaoApp`, `modeloCelular`, `sistemaOperacional` e `tiposAtivos`. Quem lê: o próprio idoso e a rede de apoio vinculada; os demais recebem 403.

- Sincronização e bateria vêm da ingestão de telemetria. Versão do app, modelo e sistema ficam `null` até o aparelho informá-los.
- `tiposAtivos` são os tipos que o idoso autoriza para a rede (projeção de `consent.changed`, destinatário `REDE`).
- Idoso que ainda não sincronizou recebe 200 com os campos do aparelho `null`.

## Testes

```bash
mvn verify
```

Precisa de Docker: os testes de integração sobem PostgreSQL e RabbitMQ com Testcontainers. O mesmo comando roda no CI (`.github/workflows/ci.yml`) em push e PR para `develop` e `main`.

## Docker

```bash
docker build -t si-telemetry-service .
docker run --rm -p 8080:8080 --env-file .env si-telemetry-service
```

Build multi-stage (Maven → JRE 21 Alpine), roda com usuário não root e tem `HEALTHCHECK` em `/actuator/health`. Opções extras da JVM vão em `JAVA_TOOL_OPTIONS`. Com `--env-file`, lembre que `localhost` dentro do container é o próprio container.

### Entrada para o Docker Compose do si-infra

O compose está no PR do si-infra (si-infra#12) e ainda não chegou na `develop`. Trecho pronto para colar em `services:` do `compose.yaml`, no mesmo formato dos outros serviços de lá:

```yaml
  telemetry-service:
    build:
      context: ../si-telemetry-service
      dockerfile: Dockerfile
    restart: unless-stopped
    environment:
      SERVER_PORT: 8080
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${DB_NAME:-seniorinsight}
      DB_USERNAME: ${DB_USERNAME:-seniorinsight}
      DB_PASSWORD: ${DB_PASSWORD:-seniorinsight}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_VHOST: /
      RABBITMQ_USERNAME: ${RABBITMQ_USERNAME:-seniorinsight}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD:-seniorinsight}
      KEYCLOAK_ISSUER_URI: http://localhost:${KEYCLOAK_PORT:-8180}/realms/seniorinsight
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWKSETURI: http://keycloak:8080/realms/seniorinsight/protocol/openid-connect/certs
      OPENWEARABLES_ENABLED: ${OPENWEARABLES_ENABLED:-false}
      OPENWEARABLES_BASE_URL: ${OPENWEARABLES_BASE_URL:-http://open-wearables:8000}
      OPENWEARABLES_API_KEY: ${OPENWEARABLES_API_KEY:-}
      CARE_SERVICE_URL: http://care-service:8080
    expose:
      - "8080"
    healthcheck:
      test: ["CMD-SHELL", "wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1"]
      interval: 15s
      timeout: 3s
      retries: 5
      start_period: 60s
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      keycloak:
        condition: service_started
```

No `api-gateway`, a rota para o serviço usaria `TELEMETRY_SERVICE_URI: http://telemetry-service:8080` e `depends_on: { telemetry-service: { condition: service_healthy } }`.
