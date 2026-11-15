-- Projeção de consent.changed (destinatário REDE): quais tipos de dado o idoso autoriza para a rede de proteção.
-- Sem linha para o tipo = não autorizado.
CREATE TABLE consentimento_rede (
    idoso_id    UUID        NOT NULL,
    tipo        VARCHAR(50) NOT NULL,
    autorizado  BOOLEAN     NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_consentimento_rede PRIMARY KEY (idoso_id, tipo),
    CONSTRAINT ck_consentimento_rede_tipo CHECK (tipo IN ('BATIMENTOS', 'PASSOS', 'SONO', 'OXIGENACAO', 'LOCALIZACAO'))
);

COMMENT ON COLUMN consentimento_rede.ocorrido_em IS 'Horário do evento no care-service; eventos mais antigos que o gravado são ignorados';
