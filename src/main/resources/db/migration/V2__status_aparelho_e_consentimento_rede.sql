ALTER TABLE status_aparelho
    ADD COLUMN modelo_celular VARCHAR(100),
    ADD COLUMN sistema_operacional VARCHAR(100);

CREATE TABLE consentimento_rede (
    idoso_id UUID NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    autorizado BOOLEAN NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_consentimento_rede PRIMARY KEY (idoso_id, tipo),
    CONSTRAINT ck_consentimento_rede_tipo CHECK (
        tipo IN ('BATIMENTOS', 'PASSOS', 'SONO', 'OXIGENACAO', 'LOCALIZACAO')
    )
);
