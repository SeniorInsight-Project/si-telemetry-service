CREATE INDEX ix_status_aparelho_sincronizacao
    ON status_aparelho (ultima_sincronizacao_em)
    WHERE sinal_perdido_desde IS NULL AND ultima_sincronizacao_em IS NOT NULL;
