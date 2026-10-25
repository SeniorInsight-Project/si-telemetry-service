CREATE TABLE telemetria_wearable (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idoso_id UUID NOT NULL,
    dispositivo_id VARCHAR(100),
    modelo_dispositivo VARCHAR(100),
    provedor VARCHAR(50),
    frequencia_cardiaca INTEGER,
    frequencia_cardiaca_repouso INTEGER,
    variabilidade_cardiaca DOUBLE PRECISION,
    passos INTEGER,
    distancia_metros DOUBLE PRECISION,
    calorias_queimadas DOUBLE PRECISION,
    queda_detectada BOOLEAN NOT NULL DEFAULT FALSE,
    forca_impacto_g DOUBLE PRECISION,
    confianca_queda DOUBLE PRECISION,
    nivel_bateria INTEGER,
    saturacao_oxigenio DOUBLE PRECISION,
    dados_brutos JSONB,
    coletado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_telemetria_wearable_idoso_coletado ON telemetria_wearable(idoso_id, coletado_em DESC);
CREATE INDEX idx_telemetria_wearable_coletado_em ON telemetria_wearable(coletado_em DESC);
CREATE INDEX idx_telemetria_wearable_dispositivo ON telemetria_wearable(dispositivo_id);
CREATE INDEX idx_telemetria_wearable_queda ON telemetria_wearable(queda_detectada) WHERE queda_detectada = TRUE;
