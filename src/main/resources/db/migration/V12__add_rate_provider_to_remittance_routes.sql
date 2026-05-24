-- Agregar proveedor de tasa de cambio a remittance_routes
-- BINANCE  → flujo Binance P2P (scheduler, actualmente deshabilitado)
-- ASTROPAY → flujo Finnhub WebSocket + consulta AstroPay
ALTER TABLE public.remittance_routes
    ADD COLUMN IF NOT EXISTS rate_provider varchar(20) NOT NULL DEFAULT 'BINANCE';

COMMENT ON COLUMN public.remittance_routes.rate_provider IS 'Proveedor de tasa de cambio: BINANCE | ASTROPAY';
