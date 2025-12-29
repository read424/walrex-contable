-- ========================================
-- TABLA 1: Países habilitados para remesas
-- ========================================
CREATE TABLE IF NOT EXISTS public.remittance_countries (
    id SERIAL PRIMARY KEY,
    id_country INTEGER NOT NULL UNIQUE,
    is_active CHAR(1) NOT NULL DEFAULT '1',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_remittance_country FOREIGN KEY (id_country)
        REFERENCES public.country(id)
);

COMMENT ON TABLE public.remittance_countries IS 'Países que tienen servicio de remesas habilitado';
COMMENT ON COLUMN public.remittance_countries.id_country IS 'FK a tabla country';
COMMENT ON COLUMN public.remittance_countries.is_active IS '1=país activo para remesas, 0=deshabilitado';

-- ========================================
-- TABLA 2: Rutas de remesas usando country_currencies
-- ========================================
CREATE TABLE IF NOT EXISTS public.remittance_routes (
    id SERIAL PRIMARY KEY,
    id_remittance_country INTEGER NOT NULL,
    id_country_currencies_from BIGINT NOT NULL,
    id_country_currencies_to BIGINT NOT NULL,
    intermediary_asset VARCHAR(10) NOT NULL DEFAULT 'USDT',
    is_active CHAR(1) NOT NULL DEFAULT '1',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_remittance_route_country FOREIGN KEY (id_remittance_country)
        REFERENCES public.remittance_countries(id),
    CONSTRAINT fk_remittance_route_cc_from FOREIGN KEY (id_country_currencies_from)
        REFERENCES public.country_currencies(id),
    CONSTRAINT fk_remittance_route_cc_to FOREIGN KEY (id_country_currencies_to)
        REFERENCES public.country_currencies(id),
    CONSTRAINT uk_remittance_route UNIQUE (id_country_currencies_from, id_country_currencies_to, intermediary_asset),
    CONSTRAINT ck_remittance_route_different_cc CHECK (id_country_currencies_from <> id_country_currencies_to)
);

COMMENT ON TABLE public.remittance_routes IS 'Rutas de remesas: pares país-moneda origen/destino';
COMMENT ON COLUMN public.remittance_routes.id_remittance_country IS 'País origen habilitado (FK a remittance_countries)';
COMMENT ON COLUMN public.remittance_routes.id_country_currencies_from IS 'Origen: FK a country_currencies (ej: Peru-PEN, Peru-USD)';
COMMENT ON COLUMN public.remittance_routes.id_country_currencies_to IS 'Destino: FK a country_currencies (ej: Venezuela-VES)';
COMMENT ON COLUMN public.remittance_routes.intermediary_asset IS 'Activo intermediario (ej: USDT, BTC)';
COMMENT ON COLUMN public.remittance_routes.is_active IS '1=ruta activa, 0=deshabilitada';

-- ========================================
-- DATOS INICIALES: Peru -> Venezuela
-- ========================================

-- Paso 1: Habilitar países para remesas
-- Asumiendo Peru=1, Venezuela=2 en tabla country (ajustar según tus IDs reales)
INSERT INTO public.remittance_countries (id_country, is_active)
VALUES
    (1, '1'),  -- Peru
    (2, '1')   -- Venezuela
ON CONFLICT (id_country) DO NOTHING;

-- Paso 2: Crear rutas usando country_currencies
-- IMPORTANTE: Ajusta los IDs de country_currencies según tu base de datos
-- Ejemplo asumiendo:
--   - Peru (id_country=1) tiene PEN (id=10) y USD (id=11) en country_currencies
--   - Venezuela (id_country=2) tiene VES (id=20) en country_currencies

-- Ruta 1: Peru-PEN -> Venezuela-VES
INSERT INTO public.remittance_routes (id_remittance_country, id_country_currencies_from, id_country_currencies_to, intermediary_asset, is_active)
SELECT
    rc.id,
    cc_from.id,
    cc_to.id,
    'USDT',
    '1'
FROM public.remittance_countries rc
CROSS JOIN public.country_currencies cc_from
CROSS JOIN public.country_currencies cc_to
WHERE rc.id_country = 1                                    -- Peru en remittance_countries
  AND cc_from.country_id = 1 AND cc_from.currency_id = 4  -- Peru-PEN
  AND cc_to.country_id = 2 AND cc_to.currency_id = 3      -- Venezuela-VES
ON CONFLICT (id_country_currencies_from, id_country_currencies_to, intermediary_asset) DO NOTHING;

-- Ruta 2: Venezuela-VES -> Peru-PEN
INSERT INTO public.remittance_routes (id_remittance_country, id_country_currencies_from, id_country_currencies_to, intermediary_asset, is_active)
SELECT
    rc.id,
    cc_from.id,
    cc_to.id,
    'USDT',
    '1'
FROM public.remittance_countries rc
CROSS JOIN public.country_currencies cc_from
CROSS JOIN public.country_currencies cc_to
WHERE rc.id_country = 2                                    -- Venezuela en remittance_countries
  AND cc_from.country_id = 2 AND cc_from.currency_id = 3  -- Venezuela-VES
  AND cc_to.country_id = 1 AND cc_to.currency_id = 4      -- Peru-PEN
ON CONFLICT (id_country_currencies_from, id_country_currencies_to, intermediary_asset) DO NOTHING;

-- ========================================
-- ÍNDICES PARA OPTIMIZACIÓN
-- ========================================
CREATE INDEX IF NOT EXISTS idx_remittance_countries_active ON public.remittance_countries(is_active);
CREATE INDEX IF NOT EXISTS idx_remittance_routes_active ON public.remittance_routes(is_active);
CREATE INDEX IF NOT EXISTS idx_remittance_routes_cc_from ON public.remittance_routes(id_country_currencies_from);
CREATE INDEX IF NOT EXISTS idx_remittance_routes_cc_to ON public.remittance_routes(id_country_currencies_to);
