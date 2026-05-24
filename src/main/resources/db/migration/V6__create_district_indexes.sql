CREATE INDEX IF NOT EXISTS idx_district_name_lower
    ON district (LOWER(name_district));