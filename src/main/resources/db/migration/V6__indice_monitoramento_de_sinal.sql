CREATE INDEX idx_status_aparelho_sinal_pendente ON status_aparelho (ultima_sincronizacao_em) WHERE sinal_perdido_desde IS NULL;
