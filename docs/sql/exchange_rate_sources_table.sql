-- Tabla para fuentes/tipos de tasas de cambio por país y moneda
CREATE TABLE public.exchange_rate_sources (
    id SERIAL PRIMARY KEY,
    id_country INTEGER NOT NULL,
    id_currency INTEGER NOT NULL,
    source_code VARCHAR(20) NOT NULL, -- 'BCV', 'PARALELO', 'BLUE', 'OFICIAL', etc.
    source_name VARCHAR(50) NOT NULL, -- 'Banco Central de Venezuela', 'Dólar Blue', etc.
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_exchange_rate_sources_country 
        FOREIGN KEY (id_country) REFERENCES country(id),
    CONSTRAINT fk_exchange_rate_sources_currency 
        FOREIGN KEY (id_currency) REFERENCES currencies(id),
    
    -- Unique constraint: un tipo de fuente por país/moneda
    CONSTRAINT uniq_exchange_rate_sources_country_currency_source 
        UNIQUE (id_country, id_currency, source_code)
);

-- Datos iniciales de ejemplo
INSERT INTO exchange_rate_sources (id_country, id_currency, source_code, source_name, description, display_order) VALUES
-- Venezuela - Bolívares
((SELECT id FROM country WHERE code_iso2 = 'VE'), (SELECT id FROM currencies WHERE code_iso3 = 'VES'), 'BCV', 'BCV', 'Banco Central de Venezuela', 1),
((SELECT id FROM country WHERE code_iso2 = 'VE'), (SELECT id FROM currencies WHERE code_iso3 = 'VES'), 'PARALELO', 'Paralelo', 'Tasa paralela del mercado venezolano', 2),

-- Argentina - Pesos
((SELECT id FROM country WHERE code_iso2 = 'AR'), (SELECT id FROM currencies WHERE code_iso3 = 'ARS'), 'OFICIAL', 'Oficial', 'Tasa oficial del Banco Central de Argentina', 1),
((SELECT id FROM country WHERE code_iso2 = 'AR'), (SELECT id FROM currencies WHERE code_iso3 = 'ARS'), 'BLUE', 'Dólar Blue', 'Tasa no oficial del dólar blue argentino', 2),

-- Colombia - Pesos (solo oficial)
((SELECT id FROM country WHERE code_iso2 = 'CO'), (SELECT id FROM currencies WHERE code_iso3 = 'COP'), 'OFICIAL', 'Oficial', 'Tasa oficial del Banco de la República de Colombia', 1),

-- Perú - Soles (solo oficial)
((SELECT id FROM country WHERE code_iso2 = 'PE'), (SELECT id FROM currencies WHERE code_iso3 = 'PEN'), 'OFICIAL', 'Oficial', 'Tasa oficial del Banco Central de Reserva del Perú', 1)

ON CONFLICT (id_country, id_currency, source_code) DO NOTHING;

-- Actualizar tabla price_exchange para usar exchange_rate_sources
ALTER TABLE price_exchange 
DROP COLUMN IF EXISTS exchange_rate_type_id,
ADD COLUMN IF NOT EXISTS exchange_rate_source_id INTEGER REFERENCES exchange_rate_sources(id);

-- Índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_exchange_rate_sources_country_currency 
ON exchange_rate_sources(id_country, id_currency, is_active);

CREATE INDEX IF NOT EXISTS idx_exchange_rate_sources_active_order 
ON exchange_rate_sources(is_active, display_order);

CREATE INDEX IF NOT EXISTS idx_price_exchange_source_date 
ON price_exchange(exchange_rate_source_id, date_exchange DESC);

-- Trigger para updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_exchange_rate_sources_updated_at ON exchange_rate_sources;
CREATE TRIGGER update_exchange_rate_sources_updated_at 
    BEFORE UPDATE ON exchange_rate_sources 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();