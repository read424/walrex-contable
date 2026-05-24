CREATE INDEX IF NOT EXISTS idx_provincia_name_lower
    ON province (LOWER(name_province));