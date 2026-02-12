-- V10: Cambiar FKs de price_exchange para apuntar a country_currencies en vez de currencies
-- Esto permite que cada país tenga su propio registro de tasa de cambio

-- Paso 1: Cambiar tipo de columnas de INTEGER a BIGINT (country_currencies.id es BIGSERIAL)
ALTER TABLE price_exchange ALTER COLUMN id_currency_base TYPE BIGINT;
ALTER TABLE price_exchange ALTER COLUMN id_currency_quote TYPE BIGINT;

-- Paso 2: Eliminar FKs antiguas que apuntan a currencies
ALTER TABLE price_exchange DROP CONSTRAINT IF EXISTS fk_price_exchange_currency_base;
ALTER TABLE price_exchange DROP CONSTRAINT IF EXISTS fk_price_exchange_currency_quote;

-- Paso 3: Agregar FKs nuevas apuntando a country_currencies
ALTER TABLE price_exchange
ADD CONSTRAINT fk_price_exchange_cc_base
FOREIGN KEY (id_currency_base) REFERENCES country_currencies(id);

ALTER TABLE price_exchange
ADD CONSTRAINT fk_price_exchange_cc_quote
FOREIGN KEY (id_currency_quote) REFERENCES country_currencies(id);

-- Paso 4: Recrear índice para las columnas actualizadas
DROP INDEX IF EXISTS idx_price_exchange_currencies_date;
CREATE INDEX idx_price_exchange_currencies_date
ON price_exchange (id_currency_base, id_currency_quote, date_exchange);
