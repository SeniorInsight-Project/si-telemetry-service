CREATE TABLE device_sync_status (
    idoso_id UUID PRIMARY KEY,
    last_sync_at TIMESTAMP WITH TIME ZONE NOT NULL,
    battery_percentage INTEGER CHECK (battery_percentage IS NULL OR battery_percentage BETWEEN 0 AND 100),
    app_version VARCHAR(50),
    active_data_types JSONB NOT NULL DEFAULT '[]'::jsonb
);
