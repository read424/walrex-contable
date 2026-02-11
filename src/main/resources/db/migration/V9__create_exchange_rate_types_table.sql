-- Crear tabla exchange_rate_types para manejar tipos de tasa de cambio por país
CREATE TABLE IF NOT EXISTS exchange_rate_types (
    id SERIAL PRIMARY KEY,
    country_id INTEGER NOT NULL,
    date_rate DATE NOT NULL DEFAULT CURRENT_DATE,
    code_rate VARCHAR(20) NOT NULL,
    name_rate VARCHAR(100) NOT NULL,
    rate_value DECIMAL(15,6) NOT NULL,
    base_currency_id INTEGER,
    is_active CHAR(1) NOT NULL DEFAULT '1',
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_exchange_rate_types_country FOREIGN KEY (country_id) REFERENCES country(id),
    CONSTRAINT fk_exchange_rate_types_currency FOREIGN KEY (base_currency_id) REFERENCES currencies(id),
    CONSTRAINT chk_exchange_rate_types_value_positive CHECK (rate_value > 0),
    CONSTRAINT chk_exchange_rate_types_active CHECK (is_active IN ('0', '1')),
    CONSTRAINT uk_exchange_rate_types_unique UNIQUE (country_id, code_rate, date_rate)
);

-- Índices para optimizar consultas
CREATE INDEX idx_exchange_rate_types_country_active ON exchange_rate_types(country_id, is_active, date_rate DESC);
CREATE INDEX idx_exchange_rate_types_code ON exchange_rate_types(code_rate);

-- Datos de ejemplo para Venezuela
INSERT INTO exchange_rate_types (country_id, code_rate, name_rate, rate_value, base_currency_id, display_order) 
SELECT 
    c.id,
    'BCV',
    'Tasa BCV Oficial',
    36.5000,
    cur.id,
    1
FROM country c
CROSS JOIN currencies cur
WHERE c.code_iso2 = 'VE' AND cur.code_iso3 = 'USD'
AND NOT EXISTS (
    SELECT 1 FROM exchange_rate_types ert 
    WHERE ert.country_id = c.id AND ert.code_rate = 'BCV'
);

INSERT INTO exchange_rate_types (country_id, code_rate, name_rate, rate_value, base_currency_id, display_order) 
SELECT 
    c.id,
    'PARALELO',
    'Dólar Paralelo',
    42.8000,
    cur.id,
    2
FROM country c
CROSS JOIN currencies cur
WHERE c.code_iso2 = 'VE' AND cur.code_iso3 = 'USD'
AND NOT EXISTS (
    SELECT 1 FROM exchange_rate_types ert 
    WHERE ert.country_id = c.id AND ert.code_rate = 'PARALELO'
);

-- Datos de ejemplo para Argentina
INSERT INTO exchange_rate_types (country_id, code_rate, name_rate, rate_value, base_currency_id, display_order) 
SELECT 
    c.id,
    'OFICIAL',
    'Dólar Oficial',
    350.0000,
    cur.id,
    1
FROM country c
CROSS JOIN currencies cur
WHERE c.code_iso2 = 'AR' AND cur.code_iso3 = 'USD'
AND NOT EXISTS (
    SELECT 1 FROM exchange_rate_types ert 
    WHERE ert.country_id = c.id AND ert.code_rate = 'OFICIAL'
);

INSERT INTO exchange_rate_types (country_id, code_rate, name_rate, rate_value, base_currency_id, display_order) 
SELECT 
    c.id,
    'DOLAR_BLUE',
    'Dólar Blue',
    480.0000,
    cur.id,
    2
FROM country c
CROSS JOIN currencies cur
WHERE c.code_iso2 = 'AR' AND cur.code_iso3 = 'USD'
AND NOT EXISTS (
    SELECT 1 FROM exchange_rate_types ert 
    WHERE ert.country_id = c.id AND ert.code_rate = 'DOLAR_BLUE'
);