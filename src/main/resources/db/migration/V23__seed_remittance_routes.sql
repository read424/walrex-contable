-- Seed data for remittance_routes table
INSERT INTO remittance_routes (
    id_remittance_country,
    id_country_currencies_from,
    id_country_currencies_to,
    intermediary_asset,
    rate_provider,
    is_active,
    created_at,
    updated_at
) VALUES (
    1,
    1,
    3,
    'USDT',
    'BINANCE',
    '1',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
