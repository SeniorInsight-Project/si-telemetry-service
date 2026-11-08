-- Estrutura inicial do telemetry-service. Roda no schema "telemetry" (spring.flyway.default-schema).

-- Amostras dos wearables, particionadas por mês do horário da medição (UTC).
CREATE TABLE medicao (
    id          UUID             NOT NULL DEFAULT gen_random_uuid(),
    idoso_id    UUID             NOT NULL,
    tipo        VARCHAR(50)      NOT NULL,
    valor       DOUBLE PRECISION NOT NULL,
    unidade     VARCHAR(20)      NOT NULL,
    medido_em   TIMESTAMPTZ      NOT NULL,
    origem      VARCHAR(100)     NOT NULL,
    recebido_em TIMESTAMPTZ      NOT NULL DEFAULT now(),
    -- Em tabela particionada, PK e UNIQUE precisam incluir a chave de partição.
    CONSTRAINT pk_medicao PRIMARY KEY (id, medido_em),
    -- Deduplicação por idoso + tipo + horário + origem (RF-019); também serve às consultas por idoso e tipo.
    CONSTRAINT uk_medicao_leitura UNIQUE (idoso_id, tipo, medido_em, origem)
) PARTITION BY RANGE (medido_em);

COMMENT ON COLUMN medicao.tipo IS 'Tipo do sinal na origem, ex.: heart_rate, steps';
COMMENT ON COLUMN medicao.origem IS 'Provedor que gerou a amostra, ex.: apple, garmin';

-- Recebe o que estiver fora das partições mensais (ex.: dados antigos).
CREATE TABLE medicao_padrao PARTITION OF medicao DEFAULT;

-- Cria a partição medicao_AAAA_MM do mês de "mes", se ainda não existir. Devolve true se criou.
CREATE FUNCTION criar_particao_medicao(mes DATE) RETURNS BOOLEAN
    LANGUAGE plpgsql AS
$$
DECLARE
    inicio DATE        := date_trunc('month', mes)::DATE;
    nome   TEXT        := 'medicao_' || to_char(inicio, 'YYYY_MM');
    de     TIMESTAMPTZ := inicio::TIMESTAMP AT TIME ZONE 'UTC';
    ate    TIMESTAMPTZ := (inicio + INTERVAL '1 month')::TIMESTAMP AT TIME ZONE 'UTC';
BEGIN
    IF to_regclass(quote_ident(nome)) IS NOT NULL THEN
        RETURN FALSE;
    END IF;
    EXECUTE format('CREATE TABLE %I PARTITION OF medicao FOR VALUES FROM (%L) TO (%L)', nome, de, ate);
    RETURN TRUE;
END;
$$;

-- Mês atual e os dois seguintes; o serviço mantém a janela (ManutencaoParticoesMedicao).
SELECT criar_particao_medicao((date_trunc('month', now() AT TIME ZONE 'UTC') + make_interval(months => m))::DATE)
  FROM generate_series(0, 2) AS m;

-- Resumo diário por sinal (RF-025), base da linha de base de 14 dias (RF-023).
CREATE TABLE agregado_diario (
    idoso_id      UUID             NOT NULL,
    dia           DATE             NOT NULL,
    tipo          VARCHAR(50)      NOT NULL,
    unidade       VARCHAR(20)      NOT NULL,
    quantidade    INTEGER          NOT NULL DEFAULT 0,
    minimo        DOUBLE PRECISION,
    maximo        DOUBLE PRECISION,
    media         DOUBLE PRECISION,
    soma          DOUBLE PRECISION,
    atualizado_em TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT pk_agregado_diario PRIMARY KEY (idoso_id, dia, tipo),
    CONSTRAINT ck_agregado_diario_quantidade CHECK (quantidade >= 0)
);

COMMENT ON COLUMN agregado_diario.dia IS 'Dia no fuso do idoso';

-- Último estado do celular do idoso (RF-020) e da sincronização com o Open Wearables.
CREATE TABLE status_aparelho (
    idoso_id                UUID        NOT NULL,
    nivel_bateria           SMALLINT,
    versao_app              VARCHAR(50),
    informado_em            TIMESTAMPTZ,
    ultima_sincronizacao_em TIMESTAMPTZ,
    sinal_perdido_desde     TIMESTAMPTZ,
    atualizado_em           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_status_aparelho PRIMARY KEY (idoso_id),
    CONSTRAINT ck_status_aparelho_bateria CHECK (nivel_bateria BETWEEN 0 AND 100)
);

COMMENT ON COLUMN status_aparelho.informado_em IS 'Horário do aparelho no último POST de status';
COMMENT ON COLUMN status_aparelho.sinal_perdido_desde IS 'Preenchido ao publicar telemetry.signal.lost; limpo quando os dados voltam';

CREATE TABLE outbox_evento (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    tipo_agregado VARCHAR(100) NOT NULL,
    agregado_id   UUID         NOT NULL,
    tipo_evento   VARCHAR(150) NOT NULL,
    payload       JSONB        NOT NULL,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    publicado_em  TIMESTAMPTZ,
    tentativas    INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_outbox_evento PRIMARY KEY (id)
);

COMMENT ON COLUMN outbox_evento.tipo_evento IS 'Routing key na exchange si.events, ex.: telemetry.ingested';

-- O relay só lê pendentes, em ordem de criação.
CREATE INDEX ix_outbox_evento_pendentes ON outbox_evento (criado_em) WHERE publicado_em IS NULL;

-- Eventos já processados da fila telemetry.q: a PK no id do evento torna o consumidor idempotente.
CREATE TABLE evento_recebido (
    id          UUID         NOT NULL,
    tipo_evento VARCHAR(150) NOT NULL,
    recebido_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_evento_recebido PRIMARY KEY (id)
);

COMMENT ON COLUMN evento_recebido.id IS 'message_id da mensagem (id da outbox de quem publicou)';
