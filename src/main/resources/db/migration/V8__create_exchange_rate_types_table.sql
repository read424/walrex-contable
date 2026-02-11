-- Crear tabla para tipos de tasas de cambio
CREATE TABLE IF NOT EXISTS exchange_rate_types (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insertar tipos de tasa iniciales
INSERT INTO exchange_rate_types (code, name, description, display_order) VALUES
('BCV', 'BCV', 'Banco Central de Venezuela', 1),
('PARALELO', 'Paralelo', 'Tasa paralela del mercado', 2),
('MARKET', 'Mercado', 'Tasa de mercado general', 3),
('EURO', 'Euro', 'Tasa basada en Euro', 4)
ON CONFLICT (code) DO NOTHING;

-- Agregar columna exchange_rate_type_id a price_exchange si no existe
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'price_exchange' 
                   AND column_name = 'exchange_rate_type_id') THEN
        ALTER TABLE price_exchange 
        ADD COLUMN exchange_rate_type_id INTEGER REFERENCES exchange_rate_types(id);
    END IF;
END $$;

-- Agregar foreign keys si no existen
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_price_exchange_currency_base') THEN
        ALTER TABLE price_exchange 
        ADD CONSTRAINT fk_price_exchange_currency_base 
        FOREIGN KEY (id_currency_base) REFERENCES currencies(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_price_exchange_currency_quote') THEN
        ALTER TABLE price_exchange 
        ADD CONSTRAINT fk_price_exchange_currency_quote 
        FOREIGN KEY (id_currency_quote) REFERENCES currencies(id);
    END IF;
END $$;

-- Crear índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_price_exchange_currencies_date 
ON price_exchange(id_currency_base, id_currency_quote, date_exchange);

CREATE INDEX IF NOT EXISTS idx_price_exchange_active_date 
ON price_exchange(is_active, date_exchange DESC);

CREATE INDEX IF NOT EXISTS idx_price_exchange_type_active 
ON price_exchange(exchange_rate_type_id, is_active);

CREATE INDEX IF NOT EXISTS idx_exchange_rate_types_active 
ON exchange_rate_types(is_active, display_order);

-- Actualizar timestamp automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Crear trigger para exchange_rate_types si no existe
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_exchange_rate_types_updated_at') THEN
        CREATE TRIGGER update_exchange_rate_types_updated_at 
        BEFORE UPDATE ON exchange_rate_types 
        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;