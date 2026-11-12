
CREATE TABLE exclusao_confirmada (
    protocolo UUID PRIMARY KEY
);

-- A impressão é usada só para impedir que mensagens antigas restaurem os dados excluídos.
CREATE TABLE titular_excluido (
    chave BYTEA PRIMARY KEY
);

CREATE TABLE usuario_open_wearables (
    idoso_id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL UNIQUE
);

CREATE TABLE telemetria_anonima (
    conjunto VARCHAR(20) NOT NULL CHECK (conjunto IN ('MEDICAO', 'AGREGADO')),
    categoria VARCHAR(30) NOT NULL CHECK (categoria IN
        ('heart_rate', 'resting_heart_rate', 'steps', 'oxygen_saturation', 'sleep', 'outro')),
    quantidade BIGINT NOT NULL CHECK (quantidade > 0),
    PRIMARY KEY (conjunto, categoria)
);

CREATE FUNCTION impedir_recriacao_titular_excluido() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    chave_titular text := TG_ARGV[1] || (to_jsonb(NEW) ->> TG_ARGV[0]);
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended(chave_titular, 0));
    IF EXISTS (SELECT 1 FROM titular_excluido WHERE chave = sha256(convert_to(chave_titular, 'UTF8'))) THEN
        RAISE EXCEPTION 'Dados de titular excluído não podem ser recriados' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER impedir_recriacao_medicao_idoso_id
    BEFORE INSERT OR UPDATE OF idoso_id ON medicao
    FOR EACH ROW EXECUTE FUNCTION impedir_recriacao_titular_excluido('idoso_id', '');

CREATE TRIGGER impedir_recriacao_agregado_diario_idoso_id
    BEFORE INSERT OR UPDATE OF idoso_id ON agregado_diario
    FOR EACH ROW EXECUTE FUNCTION impedir_recriacao_titular_excluido('idoso_id', '');

CREATE TRIGGER impedir_recriacao_status_aparelho_idoso_id
    BEFORE INSERT OR UPDATE OF idoso_id ON status_aparelho
    FOR EACH ROW EXECUTE FUNCTION impedir_recriacao_titular_excluido('idoso_id', '');

CREATE TRIGGER impedir_recriacao_consentimento_rede_idoso_id
    BEFORE INSERT OR UPDATE OF idoso_id ON consentimento_rede
    FOR EACH ROW EXECUTE FUNCTION impedir_recriacao_titular_excluido('idoso_id', '');

CREATE TRIGGER impedir_recriacao_usuario_open_wearables_idoso_id
    BEFORE INSERT OR UPDATE OF idoso_id ON usuario_open_wearables
    FOR EACH ROW EXECUTE FUNCTION impedir_recriacao_titular_excluido('idoso_id', '');
