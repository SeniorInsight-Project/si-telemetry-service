-- Modelo e sistema do celular, expostos em GET /v1/idosos/{id}/status (HU-027).
ALTER TABLE status_aparelho
    ADD COLUMN modelo_celular      VARCHAR(100),
    ADD COLUMN sistema_operacional VARCHAR(100);
